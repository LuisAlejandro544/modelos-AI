package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.LocalAiModel
import com.example.model.ModelFormatType

@Entity(tableName = "local_models")
data class ModelEntity(
  @PrimaryKey
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
  val formatType: String = "GGUF",
  val isUserImported: Boolean = true,
  val filePathOrUri: String? = null,
  val tokenizerPathOrUri: String? = null,
  val configPathOrUri: String? = null,
  val tokenizerConfigPathOrUri: String? = null,
  val generationConfigPathOrUri: String? = null,
  val defaultSystemPrompt: String = "Eres un asistente de Inteligencia Artificial local y privado ejecutado en Android.",
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toDomainModel(): LocalAiModel {
    val format = when {
      formatType.equals("SAFETENSORS", ignoreCase = true) -> ModelFormatType.SAFETENSORS
      formatType.equals("TFLITE", ignoreCase = true) -> ModelFormatType.TFLITE
      else -> ModelFormatType.GGUF
    }

    return LocalAiModel(
      id = id,
      name = name,
      developer = developer,
      parameterSize = parameterSize,
      quantization = quantization,
      ramRequired = ramRequired,
      speedEstimate = speedEstimate,
      recommendedFor = recommendedFor,
      isDownloaded = isDownloaded,
      downloadSize = downloadSize,
      contextLength = contextLength,
      formatType = format,
      isUserImported = isUserImported,
      filePathOrUri = filePathOrUri,
      tokenizerPathOrUri = tokenizerPathOrUri,
      configPathOrUri = configPathOrUri,
      tokenizerConfigPathOrUri = tokenizerConfigPathOrUri,
      generationConfigPathOrUri = generationConfigPathOrUri,
      defaultSystemPrompt = defaultSystemPrompt
    )
  }

  companion object {
    fun fromDomainModel(model: LocalAiModel): ModelEntity {
      return ModelEntity(
        id = model.id,
        name = model.name,
        developer = model.developer,
        parameterSize = model.parameterSize,
        quantization = model.quantization,
        ramRequired = model.ramRequired,
        speedEstimate = model.speedEstimate,
        recommendedFor = model.recommendedFor,
        isDownloaded = model.isDownloaded,
        downloadSize = model.downloadSize,
        contextLength = model.contextLength,
        formatType = model.formatType.name,
        isUserImported = model.isUserImported,
        filePathOrUri = model.filePathOrUri,
        tokenizerPathOrUri = model.tokenizerPathOrUri,
        configPathOrUri = model.configPathOrUri,
        tokenizerConfigPathOrUri = model.tokenizerConfigPathOrUri,
        generationConfigPathOrUri = model.generationConfigPathOrUri,
        defaultSystemPrompt = model.defaultSystemPrompt
      )
    }
  }
}
