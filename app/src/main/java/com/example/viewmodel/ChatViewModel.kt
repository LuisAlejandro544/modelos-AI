package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.LocalInferenceEngine
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.InferenceBackend
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel
import com.example.model.LocalModelsRepository
import com.example.model.ModelFormatType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class CurrentScreen {
  WELCOME,
  CHAT
}

data class SystemSpecs(
  val availableCores: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
  val totalMemoryMb: Long = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).coerceAtLeast(1024),
  val isOfflineModeActive: Boolean = true,
  val storageUsedFormatted: String = "2.4 GB libres"
)

data class ChatUiState(
  val currentScreen: CurrentScreen = CurrentScreen.WELCOME,
  val selectedModel: LocalAiModel = LocalModelsRepository.defaultModel,
  val customModels: List<LocalAiModel> = emptyList(),
  val parameters: InferenceParameters = InferenceParameters(),
  val messages: List<ChatMessage> = emptyList(),
  val isGenerating: Boolean = false,
  val showModelSelectorDialog: Boolean = false,
  val showParametersDialog: Boolean = false,
  val showClearChatDialog: Boolean = false,
  val showImportDialog: Boolean = false,
  val showTokenizerGuideDialog: Boolean = false,
  val systemSpecs: SystemSpecs = SystemSpecs()
) {
  val allAvailableModels: List<LocalAiModel>
    get() = customModels + LocalModelsRepository.presetModels
}

class ChatViewModel(
  private val engine: LocalInferenceEngine = LocalInferenceEngine()
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private var generationJob: Job? = null

  init {
    val initialSystemPrompt = LocalModelsRepository.defaultModel.defaultSystemPrompt
    _uiState.update {
      it.copy(
        parameters = it.parameters.copy(
          systemPrompt = initialSystemPrompt,
          cpuThreads = it.systemSpecs.availableCores.coerceAtMost(6)
        )
      )
    }
  }

  fun navigateTo(screen: CurrentScreen) {
    _uiState.update { it.copy(currentScreen = screen) }
  }

  fun showModelSelector(show: Boolean) {
    _uiState.update { it.copy(showModelSelectorDialog = show) }
  }

  fun showParameters(show: Boolean) {
    _uiState.update { it.copy(showParametersDialog = show) }
  }

  fun showImportDialog(show: Boolean) {
    _uiState.update { it.copy(showImportDialog = show) }
  }

  fun showTokenizerGuide(show: Boolean) {
    _uiState.update { it.copy(showTokenizerGuideDialog = show) }
  }

  fun showClearChatConfirm(show: Boolean) {
    _uiState.update { it.copy(showClearChatDialog = show) }
  }

  fun selectModel(model: LocalAiModel) {
    val recommendedBackend = if (model.formatType == ModelFormatType.SAFETENSORS) {
      InferenceBackend.RUST_CANDLE
    } else {
      InferenceBackend.CPP_LLAMA
    }

    _uiState.update {
      it.copy(
        selectedModel = model,
        showModelSelectorDialog = false,
        parameters = it.parameters.copy(
          systemPrompt = model.defaultSystemPrompt,
          backend = recommendedBackend
        )
      )
    }
  }

  fun importCustomModel(
    name: String,
    formatType: ModelFormatType,
    parameterSize: String,
    quantization: String,
    fileUriOrPath: String,
    tokenizerUriOrPath: String?,
    customPrompt: String
  ) {
    val estimatedRam = when {
      parameterSize.contains("135M", ignoreCase = true) -> "160 MB RAM"
      parameterSize.contains("360M", ignoreCase = true) || parameterSize.contains("0.3B", ignoreCase = true) -> "240 MB RAM"
      parameterSize.contains("500M", ignoreCase = true) || parameterSize.contains("0.5B", ignoreCase = true) -> "380 MB RAM"
      parameterSize.contains("0.6B", ignoreCase = true) || parameterSize.contains("600M", ignoreCase = true) -> "460 MB RAM"
      parameterSize.contains("1B", ignoreCase = true) || parameterSize.contains("1.1B", ignoreCase = true) || parameterSize.contains("1.2B", ignoreCase = true) -> "850 MB RAM"
      parameterSize.contains("1.5B", ignoreCase = true) -> "1.1 GB RAM"
      parameterSize.contains("2B", ignoreCase = true) || parameterSize.contains("2.6B", ignoreCase = true) -> "1.6 GB RAM"
      parameterSize.contains("3B", ignoreCase = true) || parameterSize.contains("3.8B", ignoreCase = true) -> "2.3 GB RAM"
      parameterSize.contains("7B", ignoreCase = true) || parameterSize.contains("8B", ignoreCase = true) -> "4.5 GB RAM"
      else -> "600 MB RAM"
    }

    val speedEst = when {
      parameterSize.contains("135M") || parameterSize.contains("360M") -> "~45-65 tok/s"
      parameterSize.contains("500M") || parameterSize.contains("0.5B") || parameterSize.contains("0.6B") -> "~35-50 tok/s"
      parameterSize.contains("1B") || parameterSize.contains("1.5B") -> "~25-35 tok/s"
      else -> "~15-25 tok/s"
    }

    val newModel = LocalAiModel(
      id = "custom-${UUID.randomUUID()}",
      name = name.ifBlank { "Modelo Local Personalizado" },
      developer = "Usuario / Almacenamiento Local",
      parameterSize = parameterSize.ifBlank { "500M" },
      quantization = quantization.ifBlank { if (formatType == ModelFormatType.GGUF) "Q4_K_M" else "F16" },
      ramRequired = estimatedRam,
      speedEstimate = speedEst,
      recommendedFor = "Modelo importado por el usuario (${formatType.displayName})",
      downloadSize = "Archivo Local",
      formatType = formatType,
      isUserImported = true,
      filePathOrUri = fileUriOrPath,
      tokenizerPathOrUri = tokenizerUriOrPath,
      defaultSystemPrompt = customPrompt.ifBlank { "Eres un asistente de IA ejecutándose desde un archivo de modelo local importado." }
    )

    _uiState.update {
      it.copy(
        customModels = listOf(newModel) + it.customModels,
        selectedModel = newModel,
        showImportDialog = false,
        showModelSelectorDialog = false,
        parameters = it.parameters.copy(
          systemPrompt = newModel.defaultSystemPrompt,
          backend = if (formatType == ModelFormatType.SAFETENSORS) InferenceBackend.RUST_CANDLE else InferenceBackend.CPP_LLAMA
        )
      )
    }
  }

  fun deleteCustomModel(modelId: String) {
    _uiState.update { state ->
      val updatedCustom = state.customModels.filterNot { it.id == modelId }
      val fallbackModel = if (state.selectedModel.id == modelId) {
        LocalModelsRepository.defaultModel
      } else {
        state.selectedModel
      }
      state.copy(
        customModels = updatedCustom,
        selectedModel = fallbackModel
      )
    }
  }

  fun updateParameters(newParameters: InferenceParameters) {
    _uiState.update {
      it.copy(
        parameters = newParameters,
        showParametersDialog = false
      )
    }
  }

  fun resetParameters() {
    val defaultPrompt = _uiState.value.selectedModel.defaultSystemPrompt
    _uiState.update {
      it.copy(
        parameters = InferenceParameters(
          systemPrompt = defaultPrompt,
          cpuThreads = it.systemSpecs.availableCores.coerceAtMost(6)
        )
      )
    }
  }

  fun sendMessage(userText: String) {
    if (userText.isBlank() || _uiState.value.isGenerating) return

    val currentModel = _uiState.value.selectedModel
    val currentParams = _uiState.value.parameters

    val userMessage = ChatMessage(
      role = ChatRole.USER,
      content = userText.trim()
    )

    val assistantMessageId = UUID.randomUUID().toString()
    val placeholderAssistantMessage = ChatMessage(
      id = assistantMessageId,
      role = ChatRole.ASSISTANT,
      content = "",
      isStreaming = true
    )

    _uiState.update {
      it.copy(
        messages = it.messages + userMessage + placeholderAssistantMessage,
        isGenerating = true
      )
    }

    generationJob?.cancel()
    generationJob = viewModelScope.launch {
      try {
        engine.generateResponseStream(userText, currentModel, currentParams).collect { chunk ->
          _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
              if (msg.id == assistantMessageId) {
                msg.copy(
                  content = chunk.partialText,
                  isStreaming = !chunk.isFinished,
                  metrics = chunk.metrics ?: msg.metrics
                )
              } else {
                msg
              }
            }
            state.copy(
              messages = updatedMessages,
              isGenerating = !chunk.isFinished
            )
          }
        }
      } catch (e: Exception) {
        _uiState.update { state ->
          val updatedMessages = state.messages.map { msg ->
            if (msg.id == assistantMessageId) {
              msg.copy(
                content = if (msg.content.isBlank()) "*(Generación interrumpida o error en el cálculo local)*" else msg.content,
                isStreaming = false
              )
            } else {
              msg
            }
          }
          state.copy(messages = updatedMessages, isGenerating = false)
        }
      } finally {
        _uiState.update { it.copy(isGenerating = false) }
      }
    }
  }

  fun stopGeneration() {
    generationJob?.cancel()
    _uiState.update { state ->
      val updatedMessages = state.messages.map { msg ->
        if (msg.isStreaming) {
          msg.copy(isStreaming = false)
        } else {
          msg
        }
      }
      state.copy(messages = updatedMessages, isGenerating = false)
    }
  }

  fun clearChat() {
    generationJob?.cancel()
    _uiState.update {
      it.copy(
        messages = emptyList(),
        isGenerating = false,
        showClearChatDialog = false
      )
    }
  }
}
