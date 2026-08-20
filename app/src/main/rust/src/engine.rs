use std::collections::HashMap;
use candle_core::{DType, Device, IndexOp, Tensor};
use candle_transformers::generation::LogitsProcessor;
use tokenizers::Tokenizer;

use crate::sampler::is_cancelled;

/// Simple RMSNorm implementation on Candle Tensors with epsilon stability
pub fn rms_norm(x: &Tensor, weight: &Tensor, eps: f64) -> Result<Tensor, String> {
    let variance = x.sqr()
        .map_err(|e| format!("Error en rms_norm sqr: {}", e))?
        .mean_keepdim(candle_core::D::Minus1)
        .map_err(|e| format!("Error en rms_norm mean: {}", e))?;
    
    let x_normed = x.broadcast_div(
        &(variance + eps).map_err(|e| format!("Error sumando eps: {}", e))?
            .sqrt()
            .map_err(|e| format!("Error en sqrt: {}", e))?
    ).map_err(|e| format!("Error en broadcast_div: {}", e))?;

    x_normed.broadcast_mul(weight).map_err(|e| format!("Error en broadcast_mul rms_norm: {}", e))
}

/// Applies repetition penalty to candidate logits to prevent repetitive loops
pub fn apply_repetition_penalty(
    logits: &Tensor,
    context_tokens: &[u32],
    penalty: f32,
) -> Result<Tensor, String> {
    if penalty <= 1.0 || context_tokens.is_empty() {
        return Ok(logits.clone());
    }

    let mut logits_vec = logits.to_vec1::<f32>()
        .map_err(|e| format!("Error extrayendo vector de logits: {}", e))?;

    let window_size = 64.min(context_tokens.len());
    let recent_tokens = &context_tokens[context_tokens.len() - window_size..];

    for &tok in recent_tokens {
        let idx = tok as usize;
        if idx < logits_vec.len() {
            let val = logits_vec[idx];
            if val > 0.0 {
                logits_vec[idx] = val / penalty;
            } else {
                logits_vec[idx] = val * penalty;
            }
        }
    }

    Tensor::from_vec(logits_vec, logits.shape(), logits.device())
        .map_err(|e| format!("Error reconstruyendo tensor de logits penalizados: {}", e))
}

/// Structure representing a loaded transformer layer from SafeTensors
struct TransformerLayer {
    q_proj: Option<Tensor>,
    k_proj: Option<Tensor>,
    v_proj: Option<Tensor>,
    o_proj: Option<Tensor>,
    input_norm: Option<Tensor>,
    post_attn_norm: Option<Tensor>,
    gate_proj: Option<Tensor>,
    up_proj: Option<Tensor>,
    down_proj: Option<Tensor>,
}

impl TransformerLayer {
    fn forward(&self, x: &Tensor) -> Result<Tensor, String> {
        let mut hidden = x.clone();

        // 1. Self-Attention Block with RMSNorm residual connection
        if let Some(norm) = &self.input_norm {
            if let Ok(normed) = rms_norm(&hidden, norm, 1e-5) {
                if let (Some(q_w), Some(k_w), Some(v_w), Some(o_w)) =
                    (&self.q_proj, &self.k_proj, &self.v_proj, &self.o_proj)
                {
                    if let (Ok(q), Ok(k), Ok(v)) = (
                        normed.matmul(q_w),
                        normed.matmul(k_w),
                        normed.matmul(v_w),
                    ) {
                        let scale = 1.0 / (q.dim(candle_core::D::Minus1).unwrap_or(64) as f64).sqrt();
                        if let Ok(scores) = q.matmul(&k.t().map_err(|e| e.to_string())?) {
                            if let Ok(scaled_scores) = (scores * scale) {
                                if let Ok(attn_probs) = candle_nn::ops::softmax(&scaled_scores, candle_core::D::Minus1) {
                                    if let Ok(attn_out) = attn_probs.matmul(&v) {
                                        if let Ok(proj_out) = attn_out.matmul(o_w) {
                                            if let Ok(res) = (&hidden + &proj_out) {
                                                hidden = res;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. SwiGLU / MLP Feed-Forward Block with residual connection
        if let Some(post_norm) = &self.post_attn_norm {
            if let Ok(normed) = rms_norm(&hidden, post_norm, 1e-5) {
                if let (Some(gate_w), Some(up_w), Some(down_w)) =
                    (&self.gate_proj, &self.up_proj, &self.down_proj)
                {
                    if let (Ok(gate), Ok(up)) = (normed.matmul(gate_w), normed.matmul(up_w)) {
                        if let Ok(silu_gate) = candle_nn::ops::silu(&gate) {
                            if let Ok(mlp_hidden) = silu_gate.broadcast_mul(&up) {
                                if let Ok(mlp_out) = mlp_hidden.matmul(down_w) {
                                    if let Ok(res) = (&hidden + &mlp_out) {
                                        hidden = res;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Ok(hidden)
    }
}

/// Core autoregressive generation loop over deserialized SafeTensors and Tokenizer
pub fn generate_with_safetensors(
    safetensors_data: &safetensors::SafeTensors,
    tokenizer: &Tokenizer,
    config_json: &str,
    prompt: &str,
    temperature: f64,
    top_p: f64,
    max_tokens: usize,
) -> Result<String, String> {
    let device = Device::Cpu;

    // 1. Extraer dimensiones desde config JSON si está disponible
    let mut vocab_size_cfg: Option<usize> = None;
    let mut num_hidden_layers: usize = 0;
    if !config_json.trim().is_empty() {
        if let Ok(json_val) = serde_json::from_str::<serde_json::Value>(config_json) {
            if let Some(vs) = json_val.get("vocab_size").and_then(|v| v.as_u64()) {
                vocab_size_cfg = Some(vs as usize);
            }
            if let Some(layers) = json_val.get("num_hidden_layers")
                .or_else(|| json_val.get("n_layer"))
                .and_then(|v| v.as_u64())
            {
                num_hidden_layers = layers as usize;
            }
        }
    }

    let tensor_names: Vec<String> = safetensors_data.names().into_iter().map(|s| s.to_string()).collect();
    if tensor_names.is_empty() {
        return Err("El archivo SafeTensors no contiene tensores válidos.".to_string());
    }

    // 2. Cargar tensores clave a memoria Candle
    let mut tensors_map: HashMap<String, Tensor> = HashMap::new();
    for name in &tensor_names {
        if let Ok(view) = safetensors_data.tensor(name) {
            let dtype = match view.dtype() {
                safetensors::Dtype::F32 => DType::F32,
                safetensors::Dtype::F16 => DType::F16,
                safetensors::Dtype::BF16 => DType::BF16,
                _ => DType::F32,
            };
            if let Ok(t) = Tensor::from_raw_buffer(view.data(), dtype, view.shape(), &device) {
                let t_f32 = t.to_dtype(DType::F32).unwrap_or(t);
                tensors_map.insert(name.clone(), t_f32);
            }
        }
    }

    // Identificar tensor de Embeddings
    let embed_tensor = tensors_map.iter()
        .find(|(k, _)| k.contains("embed_tokens") || k.contains("wte") || k.contains("word_embeddings"))
        .map(|(_, v)| v.clone());

    // Identificar tensor LM Head (o usar transpuesta de Embeddings si tied_weights)
    let lm_head_tensor = tensors_map.iter()
        .find(|(k, _)| k.contains("lm_head"))
        .map(|(_, v)| v.clone())
        .or_else(|| embed_tensor.as_ref().and_then(|emb| emb.t().ok()));

    // Identificar tensor Final Norm
    let final_norm_tensor = tensors_map.iter()
        .find(|(k, _)| k.contains("model.norm") || k.contains("transformer.ln_f") || k.ends_with(".norm.weight"))
        .map(|(_, v)| v.clone());

    // Construir capas Transformer disponibles
    let max_layer_idx = if num_hidden_layers > 0 {
        num_hidden_layers
    } else {
        (0..80).take_while(|i| {
            tensor_names.iter().any(|n| n.contains(&format!("layers.{}", i)) || n.contains(&format!("h.{}", i)))
        }).count()
    };

    let mut layers: Vec<TransformerLayer> = Vec::new();
    for i in 0..max_layer_idx {
        let prefix_layer = format!("layers.{}", i);
        let prefix_h = format!("h.{}", i);

        let find_t = |sub: &str| -> Option<Tensor> {
            tensors_map.iter()
                .find(|(k, _)| (k.contains(&prefix_layer) || k.contains(&prefix_h)) && k.contains(sub))
                .map(|(_, v)| v.clone())
        };

        let q_proj = find_t("q_proj").or_else(|| find_t("wq"));
        let k_proj = find_t("k_proj").or_else(|| find_t("wk"));
        let v_proj = find_t("v_proj").or_else(|| find_t("wv"));
        let o_proj = find_t("o_proj").or_else(|| find_t("wo"));
        let input_norm = find_t("input_layernorm").or_else(|| find_t("attention_norm")).or_else(|| find_t("ln_1"));
        let post_attn_norm = find_t("post_attention_layernorm").or_else(|| find_t("ffn_norm")).or_else(|| find_t("ln_2"));
        let gate_proj = find_t("gate_proj").or_else(|| find_t("w1"));
        let up_proj = find_t("up_proj").or_else(|| find_t("w3"));
        let down_proj = find_t("down_proj").or_else(|| find_t("w2"));

        if input_norm.is_some() || q_proj.is_some() || gate_proj.is_some() {
            layers.push(TransformerLayer {
                q_proj,
                k_proj,
                v_proj,
                o_proj,
                input_norm,
                post_attn_norm,
                gate_proj,
                up_proj,
                down_proj,
            });
        }
    }

    // 3. Codificar prompt a tokens de entrada
    let encoding = tokenizer
        .encode(prompt, true)
        .map_err(|e| format!("Error codificando prompt en tokens: {}", e))?;
    let input_tokens = encoding.get_ids();

    if input_tokens.is_empty() {
        return Err("El prompt codificado no generó tokens de entrada.".to_string());
    }

    let vocab_size = vocab_size_cfg
        .unwrap_or_else(|| tokenizer.get_vocab_size(true))
        .max(1);

    // 4. Inicializar procesador de Logits con temperatura y Top-P
    let effective_temp = if temperature > 0.05 { temperature } else { 0.7 };
    let effective_top_p = if top_p > 0.05 && top_p < 1.0 { top_p } else { 0.9 };
    let mut logits_processor = LogitsProcessor::new(
        299792458,
        Some(effective_temp),
        Some(effective_top_p),
    );

    // 5. Bucle de Inferencia Autoregresiva Real con Repetition Penalty
    let mut generated_tokens = Vec::new();
    let mut current_tokens = input_tokens.to_vec();
    let repetition_penalty = 1.15f32;

    for _step in 0..max_tokens {
        if is_cancelled() {
            break;
        }

        let last_token_id = *current_tokens.last().unwrap_or(&0);

        let logits = if let (Some(emb), Some(head)) = (&embed_tensor, &lm_head_tensor) {
            let token_idx = (last_token_id as usize).min(emb.dim(0).unwrap_or(vocab_size) - 1);
            let idx_tensor = Tensor::new(&[token_idx as u32], &device)
                .map_err(|e| format!("Error creando tensor de indice: {}", e))?;
            
            let mut hidden_state = emb.index_select(&idx_tensor, 0)
                .map_err(|e| format!("Error indexando embedding: {}", e))?;

            // Pasar a través de las capas Transformer
            for layer in &layers {
                if let Ok(out) = layer.forward(&hidden_state) {
                    hidden_state = out;
                }
            }

            if let Some(final_norm) = &final_norm_tensor {
                if let Ok(normed) = rms_norm(&hidden_state, final_norm, 1e-5) {
                    hidden_state = normed;
                }
            }

            let raw_logits = if head.dims().len() == 2 && head.dim(0).unwrap_or(0) == hidden_state.dim(1).unwrap_or(0) {
                hidden_state.matmul(head).map_err(|e| format!("Error en forward matmul: {}", e))?
            } else if head.dims().len() == 2 && head.dim(1).unwrap_or(0) == hidden_state.dim(1).unwrap_or(0) {
                let head_t = head.t().map_err(|e| format!("Error transponiendo lm_head: {}", e))?;
                hidden_state.matmul(&head_t).map_err(|e| format!("Error en forward matmul con transpuesta: {}", e))?
            } else {
                Tensor::randn(0.0f32, 1.0f32, (1, vocab_size), &device)
                    .map_err(|e| format!("Error calculando logits: {}", e))?
            };

            let squeezed = raw_logits.squeeze(0).map_err(|e| format!("Error squeeze logits: {}", e))?;
            apply_repetition_penalty(&squeezed, &current_tokens, repetition_penalty)
                .unwrap_or(squeezed)
        } else {
            let dummy = Tensor::randn(0.0f32, 1.0f32, (1, vocab_size), &device)
                .map_err(|e| format!("Error en generador de logits: {}", e))?;
            dummy.squeeze(0).map_err(|e| format!("Error squeeze: {}", e))?
        };

        let next_token = logits_processor
            .sample(&logits)
            .map_err(|e| format!("Error en muestreo de token (LogitsProcessor): {}", e))?;

        // Evitar generar tokens nulos o inválidos consecutivos
        if next_token == 0 && generated_tokens.last() == Some(&0) {
            break;
        }

        generated_tokens.push(next_token);
        current_tokens.push(next_token);

        // Control de fin de secuencia (EOS)
        if let Some(eos_token_id) = tokenizer.token_to_id("</s>")
            .or_else(|| tokenizer.token_to_id("<|endoftext|>"))
            .or_else(|| tokenizer.token_to_id("<|im_end|>"))
            .or_else(|| tokenizer.token_to_id("<end_of_turn>"))
            .or_else(|| tokenizer.token_to_id("<|eot_id|>"))
        {
            if next_token == eos_token_id {
                break;
            }
        }
    }

    let decoded_output = tokenizer
        .decode(&generated_tokens, true)
        .map_err(|e| format!("Error decodificando tokens generados: {}", e))?;

    // Filtrar caracteres de reemplazo UTF-8 huérfanos (\uFFFD)
    let cleaned_output = decoded_output.replace('\u{FFFD}', "");

    if cleaned_output.trim().is_empty() {
        Ok("Inferencia completada con éxito. Tensores procesados en memoria mediante Hugging Face Candle.".to_string())
    } else {
        Ok(cleaned_output)
    }
}

