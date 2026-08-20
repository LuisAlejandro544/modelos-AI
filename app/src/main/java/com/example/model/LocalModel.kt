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
    description = "1 solo archivo autocontenido: Pesos + Tokenizador embebidos."
  ),
  SAFETENSORS(
    extension = ".safetensors",
    displayName = "SafeTensors (Rust / Candle)",
    requiresSeparateTokenizer = true,
    description = "Pesos crudos en tensores + tokenizer.json + config.json por separado."
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
  val downloadSize: String = "Archivo Local",
  val contextLength: Int = 4096,
  val formatType: ModelFormatType = ModelFormatType.GGUF,
  val isUserImported: Boolean = true,
  val filePathOrUri: String? = null,
  val tokenizerPathOrUri: String? = null,
  val configPathOrUri: String? = null,
  val tokenizerConfigPathOrUri: String? = null,
  val generationConfigPathOrUri: String? = null,
  val defaultSystemPrompt: String = "Eres un asistente de Inteligencia Artificial local y privado ejecutado en Android."
)

object LocalModelsRepository {
  /**
   * Catálogo de modelos preconfigurados eliminado por directiva del usuario.
   * La aplicación opera exclusivamente con modelos reales cargados por el usuario (.gguf o .safetensors).
   */
  val presetModels: List<LocalAiModel> = emptyList()
}

