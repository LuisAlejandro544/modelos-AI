package com.example.model

enum class ModelFormatType(
  val extension: String,
  val displayName: String,
  val requiresSeparateTokenizer: Boolean,
  val description: String
) {
  GGUF(
    extension = ".gguf",
    displayName = "GGUF (llama.cpp)",
    requiresSeparateTokenizer = false,
    description = "Todo en uno: Pesos + Tokenizador embebidos. Listo para inferencia directa."
  ),
  SAFETENSORS(
    extension = ".safetensors",
    displayName = "SafeTensors (Rust / Candle)",
    requiresSeparateTokenizer = true,
    description = "Solo tensores de pesos crudos. Requiere archivo tokenizer.json o vocab.json adicional."
  )
}

data class LocalAiModel(
  val id: String,
  val name: String,
  val developer: String,
  val parameterSize: String,
  val quantization: String,
  val ramRequired: String,
  val speedEstimate: String,
  val recommendedFor: String,
  val isDownloaded: Boolean = true,
  val downloadSize: String,
  val contextLength: Int = 4096,
  val formatType: ModelFormatType = ModelFormatType.GGUF,
  val isUserImported: Boolean = false,
  val filePathOrUri: String? = null,
  val tokenizerPathOrUri: String? = null,
  val defaultSystemPrompt: String = "Eres un asistente de Inteligencia Artificial local y privado que se ejecuta en el dispositivo Android del usuario."
)

object LocalModelsRepository {
  val presetModels = listOf(
    LocalAiModel(
      id = "gemma-2-2b",
      name = "Gemma 2 2B Instruct",
      developer = "Google",
      parameterSize = "2.6B",
      quantization = "Q4_K_M",
      ramRequired = "1.6 GB RAM",
      speedEstimate = "~18-25 tok/s",
      recommendedFor = "Razonamiento balanceado, respuestas concisas y redacción en español",
      downloadSize = "1.52 GB",
      formatType = ModelFormatType.GGUF,
      defaultSystemPrompt = "Eres Gemma 2B, un modelo de lenguaje local desarrollado por Google. Responde con claridad y concisión."
    ),
    LocalAiModel(
      id = "smollm-360m",
      name = "SmolLM 360M Instruct",
      developer = "Hugging Face TB",
      parameterSize = "360M",
      quantization = "Q4_K_M / F16",
      ramRequired = "240 MB RAM",
      speedEstimate = "~45-60 tok/s",
      recommendedFor = "Ultra rápido y ligero, ideal para teléfonos de entrada y respuestas instantáneas",
      downloadSize = "225 MB",
      formatType = ModelFormatType.GGUF,
      defaultSystemPrompt = "Eres SmolLM, un asistente ultra rápido y conciso que corre localmente."
    ),
    LocalAiModel(
      id = "qwen-2.5-0.5b",
      name = "Qwen 2.5 0.5B Chat",
      developer = "Alibaba Cloud",
      parameterSize = "0.5B (490M)",
      quantization = "Q4_K_M",
      ramRequired = "380 MB RAM",
      speedEstimate = "~38-50 tok/s",
      recommendedFor = "Modelo sub-1B con excelente seguimiento de instrucciones y español fluido",
      downloadSize = "390 MB",
      formatType = ModelFormatType.GGUF,
      defaultSystemPrompt = "Eres Qwen 2.5 0.5B, un modelo compacto y eficiente ejecutándose en Android."
    ),
    LocalAiModel(
      id = "llama-3.2-1b",
      name = "Llama 3.2 1B Instruct",
      developer = "Meta",
      parameterSize = "1.2B",
      quantization = "Q4_K_M",
      ramRequired = "850 MB RAM",
      speedEstimate = "~28-36 tok/s",
      recommendedFor = "Diálogos fluidos, tareas generales y resúmenes directos",
      downloadSize = "780 MB",
      formatType = ModelFormatType.GGUF,
      defaultSystemPrompt = "Eres Llama 3.2 1B ejecutado localmente de forma privada."
    ),
    LocalAiModel(
      id = "phi-3-mini",
      name = "Phi-3 Mini 3.8B",
      developer = "Microsoft",
      parameterSize = "3.8B",
      quantization = "Q4_K_M",
      ramRequired = "2.3 GB RAM",
      speedEstimate = "~12-16 tok/s",
      recommendedFor = "Alta capacidad de razonamiento lógico, código y matemáticas",
      downloadSize = "2.18 GB",
      formatType = ModelFormatType.GGUF,
      defaultSystemPrompt = "Eres Phi-3 Mini, un modelo de alta precisión ejecutándose localmente en Android."
    ),
    LocalAiModel(
      id = "deepseek-r1-1.5b",
      name = "DeepSeek-R1 Distill 1.5B",
      developer = "DeepSeek AI",
      parameterSize = "1.5B",
      quantization = "Q4_K_M",
      ramRequired = "1.2 GB RAM",
      speedEstimate = "~20-26 tok/s",
      recommendedFor = "Razonamiento analítico profundo paso a paso sin conexión",
      downloadSize = "1.05 GB",
      formatType = ModelFormatType.GGUF,
      defaultSystemPrompt = "Eres DeepSeek-R1 Distill local. Piensa paso a paso antes de responder."
    )
  )

  val defaultModel = presetModels[0]
}
