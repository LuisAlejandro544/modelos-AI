package com.example.data.repository

import com.example.model.LocalAiModel
import com.example.model.ModelFormatType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ModelRepository {

  private val _customModels = MutableStateFlow<List<LocalAiModel>>(emptyList())
  val customModels: StateFlow<List<LocalAiModel>> = _customModels.asStateFlow()

  fun addGgufModel(uriOrPath: String, displayName: String? = null): LocalAiModel {
    val cleanName = displayName?.ifBlank { null }
      ?: uriOrPath.substringAfterLast("/").substringBeforeLast(".")
        .replace("-", " ")
        .replace("_", " ")

    val newModel = LocalAiModel(
      id = "gguf-${UUID.randomUUID()}",
      name = cleanName,
      developer = "Archivo Local GGUF",
      parameterSize = "Auto (GGUF)",
      quantization = "Q4 / Mixto",
      ramRequired = "Bajo consumo mmap",
      speedEstimate = "~25-45 tok/s (GPU)",
      recommendedFor = "Inferencia directa todo en uno (.gguf) con llama.cpp",
      downloadSize = "Local",
      formatType = ModelFormatType.GGUF,
      contextLength = 4096,
      isUserImported = true,
      filePathOrUri = uriOrPath,
      defaultSystemPrompt = "Eres un asistente de IA local ejecutado de forma privada desde tu archivo GGUF."
    )

    _customModels.update { listOf(newModel) + it }
    return newModel
  }

  fun addSafeTensorsBundle(
    modelName: String,
    weightsUri: String,
    tokenizerUri: String,
    configUri: String,
    tokenizerConfigUri: String?,
    generationConfigUri: String?,
    paramSize: String,
    quantization: String,
    customPrompt: String
  ): LocalAiModel {
    val cleanName = if (modelName.isNotBlank()) modelName else "Modelo SafeTensors (Candle)"
    val newModel = LocalAiModel(
      id = "safetensors-${UUID.randomUUID()}",
      name = cleanName,
      developer = "Archivos SafeTensors",
      parameterSize = paramSize.ifBlank { "Auto" },
      quantization = quantization.ifBlank { "F16 / BF16" },
      ramRequired = "Rust Candle optimizado",
      speedEstimate = "~20-38 tok/s (GPU)",
      recommendedFor = "Inferencia nativa modular (.safetensors + tokenizer.json + config.json)",
      downloadSize = "Local",
      formatType = ModelFormatType.SAFETENSORS,
      contextLength = 4096,
      isUserImported = true,
      filePathOrUri = weightsUri,
      tokenizerPathOrUri = tokenizerUri,
      configPathOrUri = configUri,
      tokenizerConfigPathOrUri = tokenizerConfigUri,
      generationConfigPathOrUri = generationConfigUri,
      defaultSystemPrompt = customPrompt.ifBlank { "Eres un asistente de IA ejecutado desde tensores SafeTensors en Rust." }
    )

    _customModels.update { listOf(newModel) + it }
    return newModel
  }

  fun addCustomModel(
    name: String,
    formatType: ModelFormatType,
    parameterSize: String,
    quantization: String,
    filePathOrUri: String,
    tokenizerUri: String?,
    configUri: String?,
    tokenizerConfigUri: String?,
    generationConfigUri: String?,
    customPrompt: String
  ): LocalAiModel {
    val cleanName = if (name.isNotBlank()) name else "Modelo Importado"
    val isGguf = formatType == ModelFormatType.GGUF
    val newModel = LocalAiModel(
      id = "${if (isGguf) "gguf" else "safetensors"}-${UUID.randomUUID()}",
      name = cleanName,
      developer = "Importado por usuario",
      parameterSize = parameterSize.ifBlank { "Personalizado" },
      quantization = quantization.ifBlank { if (isGguf) "Q4_K_M" else "F16" },
      ramRequired = if (isGguf) "Carga mmap optimizada" else "Rust Candle tensores",
      speedEstimate = if (isGguf) "~25-45 tok/s" else "~20-38 tok/s",
      recommendedFor = "Inferencia local personalizada",
      downloadSize = "Local",
      formatType = formatType,
      contextLength = 4096,
      isUserImported = true,
      filePathOrUri = filePathOrUri,
      tokenizerPathOrUri = tokenizerUri,
      configPathOrUri = configUri,
      tokenizerConfigPathOrUri = tokenizerConfigUri,
      generationConfigPathOrUri = generationConfigUri,
      defaultSystemPrompt = customPrompt.ifBlank { "Eres un asistente de IA local ejecutado en Android." }
    )

    _customModels.update { listOf(newModel) + it }
    return newModel
  }

  fun deleteModel(modelId: String) {
    _customModels.update { currentList ->
      currentList.filterNot { it.id == modelId }
    }
  }
}
