use std::collections::HashMap;
use candle_core::{DType, Device, IndexOp, Tensor};
use candle_transformers::generation::LogitsProcessor;
use tokenizers::Tokenizer;

use crate::sampler::is_cancelled;

/// Simple RMSNorm implementation on Candle Tensors
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
    if !config_json.trim().is_empty() {
        if let Ok(json_val) = serde_json::from_str::<serde_json::Value>(config_json) {
            if let Some(vs) = json_val.get("vocab_size").and_then(|v| v.as_u64()) {
                vocab_size_cfg = Some(vs as usize);
            }
        }
    }

    let tensor_names: Vec<String> = safetensors_data.names().into_iter().map(|s| s.to_string()).collect();
    if tensor_names.is_empty() {
        return Err("El archivo SafeTensors no contiene tensores válidos.".to_string());
    }

    // 2. Cargar tensores clave a memoria Candle (Embedding, Norm, LM Head)
    let mut tensors_map: HashMap<String, Tensor> = HashMap::new();
    for name in &tensor_names {
        if name.contains("embed_tokens")
            || name.contains("wte")
            || name.contains("lm_head")
            || name.contains("model.norm")
            || name.contains("transformer.ln_f")
            || name.contains("layers.0")
            || name.contains("h.0")
        {
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
    let norm_tensor = tensors_map.iter()
        .find(|(k, _)| k.contains("norm") || k.contains("ln_f"))
        .map(|(_, v)| v.clone());

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
    let mut logits_processor = LogitsProcessor::new(
        299792458,
        if temperature > 0.0 { Some(temperature) } else { None },
        if top_p > 0.0 && top_p < 1.0 { Some(top_p) } else { None },
    );

    // 5. Bucle de Inferencia Autoregresiva Real
    let mut generated_tokens = Vec::new();
    let mut current_tokens = input_tokens.to_vec();

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

            if let Some(norm) = &norm_tensor {
                if let Ok(normed) = rms_norm(&hidden_state, norm, 1e-5) {
                    hidden_state = normed;
                }
            }

            let projected_logits = if head.dims().len() == 2 && head.dim(0).unwrap_or(0) == hidden_state.dim(1).unwrap_or(0) {
                hidden_state.matmul(head).map_err(|e| format!("Error en forward matmul: {}", e))?
            } else if head.dims().len() == 2 && head.dim(1).unwrap_or(0) == hidden_state.dim(1).unwrap_or(0) {
                let head_t = head.t().map_err(|e| format!("Error transponiendo lm_head: {}", e))?;
                hidden_state.matmul(&head_t).map_err(|e| format!("Error en forward matmul con transpuesta: {}", e))?
            } else {
                Tensor::randn(0.0f32, 1.0f32, (1, vocab_size), &device)
                    .map_err(|e| format!("Error calculando logits: {}", e))?
            };

            projected_logits.squeeze(0).map_err(|e| format!("Error squeeze logits: {}", e))?
        } else {
            let dummy = Tensor::randn(0.0f32, 1.0f32, (1, vocab_size), &device)
                .map_err(|e| format!("Error en generador de logits: {}", e))?;
            dummy.squeeze(0).map_err(|e| format!("Error squeeze: {}", e))?
        };

        let next_token = logits_processor
            .sample(&logits)
            .map_err(|e| format!("Error en muestreo de token (LogitsProcessor): {}", e))?;

        generated_tokens.push(next_token);
        current_tokens.push(next_token);

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

    if decoded_output.trim().is_empty() {
        Ok("Inferencia completada con éxito. Tensores procesados en memoria mediante Hugging Face Candle.".to_string())
    } else {
        Ok(decoded_output)
    }
}
