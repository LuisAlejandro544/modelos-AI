package com.example.data.repository

import android.content.Context
import com.example.App
import com.example.data.local.LocalAiDatabase
import com.example.data.local.dao.ModelDao
import com.example.data.local.entities.ModelEntity
import com.example.engine.NativeCppBridge
import com.example.model.LocalAiModel
import com.example.model.ModelFormatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ModelRepository(
  private val modelDao: ModelDao? = try {
    App.instance?.let { LocalAiDatabase.getDatabase(it).modelDao() }
  } catch (_: Throwable) {
    null
  },
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

  private val _customModels = MutableStateFlow<List<LocalAiModel>>(emptyList())
  val customModels: StateFlow<List<LocalAiModel>> = _customModels.asStateFlow()

  init {
    if (modelDao != null) {
      coroutineScope.launch {
        try {
          modelDao.getAllModels().collect { entities ->
            val domainModels = entities.map { it.toDomainModel() }
            _customModels.value = domainModels
          }
        } catch (_: Throwable) {}
      }
    }
  }

  fun addGgufModel(uriOrPath: String, displayName: String? = null, context: Context? = null): LocalAiModel {
    val meta = NativeCppBridge.parseGgufMetadataSafe(uriOrPath, context)

    val cleanFallbackName = displayName?.ifBlank { null }
      ?: uriOrPath.substringAfterLast("/").substringBeforeLast(".")
        .replace("-", " ")
        .replace("_", " ")

    val modelName = when {
      !displayName.isNullOrBlank() -> displayName
      meta != null && meta.isValid && meta.modelName.isNotBlank() && meta.modelName != "GGUF Model" -> meta.modelName
      meta != null && meta.isValid && meta.architecture.isNotBlank() -> "${meta.architecture.replaceFirstChar { it.titlecase() }} GGUF"
      else -> cleanFallbackName
    }

    val contextLen = if (meta != null && meta.isValid && meta.contextLength > 0) {
      meta.contextLength.toInt().coerceIn(512, 131072)
    } else {
      4096
    }

    val arch = if (meta != null && meta.isValid && meta.architecture.isNotBlank()) meta.architecture else "llama"
    val paramSize = if (meta != null && meta.isValid && meta.blockCount > 0) {
      when {
        meta.blockCount <= 16 -> "0.5B / SLM"
        meta.blockCount <= 28 -> "1.5B - 3B"
        meta.blockCount <= 32 -> "7B / 8B"
        else -> "${meta.blockCount} capas"
      }
    } else {
      "Auto (GGUF)"
    }

    val newModel = LocalAiModel(
      id = "gguf-${UUID.randomUUID()}",
      name = modelName,
      developer = "Archivo Local GGUF (${arch.uppercase()})",
      parameterSize = paramSize,
      quantization = "Q4 / Mixto",
      ramRequired = "Bajo consumo mmap",
      speedEstimate = "~25-45 tok/s (GPU)",
      recommendedFor = if (meta != null && meta.isValid) {
        "Inferencia nativa C++ (${arch.uppercase()} | ${meta.tensorCount} tensores | v${meta.version})"
      } else {
        "Inferencia directa todo en uno (.gguf) con llama.cpp"
      },
      downloadSize = "Local",
      formatType = ModelFormatType.GGUF,
      contextLength = contextLen,
      isUserImported = true,
      filePathOrUri = uriOrPath,
      defaultSystemPrompt = "Eres un asistente de IA local ejecutado de forma privada desde tu archivo GGUF."
    )

    _customModels.update { listOf(newModel) + it.filterNot { m -> m.filePathOrUri == uriOrPath } }
    persistModel(newModel)
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
    persistModel(newModel)
    return newModel
  }

  fun updateSafeTensorsBundle(
    modelId: String,
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
    val existing = _customModels.value.find { it.id == modelId }
    val updatedModel = (existing ?: LocalAiModel(
      id = modelId,
      name = modelName,
      developer = "Archivos SafeTensors",
      parameterSize = paramSize,
      quantization = quantization,
      ramRequired = "Rust Candle",
      speedEstimate = "~20-38 tok/s",
      recommendedFor = "Inferencia modular SafeTensors",
      downloadSize = "Local",
      formatType = ModelFormatType.SAFETENSORS
    )).copy(
      name = modelName.ifBlank { "Modelo SafeTensors" },
      filePathOrUri = weightsUri,
      tokenizerPathOrUri = tokenizerUri,
      configPathOrUri = configUri,
      tokenizerConfigPathOrUri = tokenizerConfigUri,
      generationConfigPathOrUri = generationConfigUri,
      parameterSize = paramSize.ifBlank { "Auto" },
      quantization = quantization.ifBlank { "F16 / BF16" },
      defaultSystemPrompt = customPrompt.ifBlank { "Eres un asistente de IA local en Rust." },
      isUserImported = true
    )

    _customModels.update { list ->
      val index = list.indexOfFirst { it.id == modelId }
      if (index >= 0) {
        list.toMutableList().apply { set(index, updatedModel) }
      } else {
        listOf(updatedModel) + list
      }
    }
    persistModel(updatedModel)
    return updatedModel
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
    persistModel(newModel)
    return newModel
  }

  fun deleteModel(modelId: String) {
    _customModels.update { currentList ->
      currentList.filterNot { it.id == modelId }
    }
    if (modelDao != null) {
      coroutineScope.launch {
        try {
          modelDao.deleteModelById(modelId)
        } catch (_: Throwable) {}
      }
    }
  }

  private fun persistModel(model: LocalAiModel) {
    if (modelDao != null) {
      coroutineScope.launch {
        try {
          modelDao.insertModel(ModelEntity.fromDomainModel(model))
        } catch (_: Throwable) {}
      }
    }
  }
}
