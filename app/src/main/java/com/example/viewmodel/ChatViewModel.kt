package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ModelRepository
import com.example.engine.LocalInferenceEngine
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.HardwareAccelerator
import com.example.model.InferenceBackend
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel
import com.example.model.ModelFormatType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
  private val repository: ModelRepository = ModelRepository(),
  private val engine: LocalInferenceEngine = LocalInferenceEngine()
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private var generationJob: Job? = null

  init {
    // Observe repository custom models
    viewModelScope.launch {
      repository.customModels.collect { models ->
        _uiState.update { state ->
          state.copy(customModels = models)
        }
      }
    }

    _uiState.update {
      it.copy(
        parameters = it.parameters.copy(
          systemPrompt = "Eres un asistente de IA local y privado ejecutado en el dispositivo del usuario.",
          contextWindow = 4096,
          cpuThreads = it.systemSpecs.availableCores.coerceAtMost(6),
          accelerator = HardwareAccelerator.AUTO,
          useMmap = true
        )
      )
    }
  }

  // Navigation & Dialog Controls
  fun navigateTo(screen: CurrentScreen) {
    _uiState.update { it.copy(currentScreen = screen) }
  }

  fun showModelSelector(show: Boolean) {
    _uiState.update { it.copy(showModelSelectorDialog = show) }
  }

  fun showParameters(show: Boolean) {
    _uiState.update { it.copy(showParametersDialog = show) }
  }

  fun showTokenizerGuide(show: Boolean) {
    _uiState.update { it.copy(showTokenizerGuideDialog = show) }
  }

  fun showImportDialog(show: Boolean) {
    _uiState.update { it.copy(showImportDialog = show) }
  }

  fun showClearChatConfirm(show: Boolean) {
    _uiState.update { it.copy(showClearChatDialog = show) }
  }

  // Model Management
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
          contextWindow = model.contextLength,
          backend = recommendedBackend
        )
      )
    }
  }

  fun loadGgufModelDirect(uriOrPath: String, displayName: String? = null) {
    val newModel = repository.addGgufModel(uriOrPath, displayName)
    _uiState.update {
      it.copy(
        selectedModel = newModel,
        currentScreen = CurrentScreen.CHAT,
        parameters = it.parameters.copy(
          systemPrompt = newModel.defaultSystemPrompt,
          contextWindow = newModel.contextLength,
          backend = InferenceBackend.CPP_LLAMA
        )
      )
    }
  }

  fun importSafeTensorsBundle(
    modelName: String,
    weightsUri: String,
    tokenizerUri: String,
    configUri: String,
    tokenizerConfigUri: String?,
    generationConfigUri: String?,
    paramSize: String,
    quantization: String,
    customPrompt: String
  ) {
    val newModel = repository.addSafeTensorsBundle(
      modelName = modelName,
      weightsUri = weightsUri,
      tokenizerUri = tokenizerUri,
      configUri = configUri,
      tokenizerConfigUri = tokenizerConfigUri,
      generationConfigUri = generationConfigUri,
      paramSize = paramSize,
      quantization = quantization,
      customPrompt = customPrompt
    )

    _uiState.update {
      it.copy(
        selectedModel = newModel,
        currentScreen = CurrentScreen.CHAT,
        parameters = it.parameters.copy(
          systemPrompt = newModel.defaultSystemPrompt,
          contextWindow = newModel.contextLength,
          backend = InferenceBackend.RUST_CANDLE
        )
      )
    }
  }

  fun importCustomModel(
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
  ) {
    val newModel = repository.addCustomModel(
      name = name,
      formatType = formatType,
      parameterSize = parameterSize,
      quantization = quantization,
      filePathOrUri = filePathOrUri,
      tokenizerUri = tokenizerUri,
      configUri = configUri,
      tokenizerConfigUri = tokenizerConfigUri,
      generationConfigUri = generationConfigUri,
      customPrompt = customPrompt
    )

    _uiState.update {
      it.copy(
        selectedModel = newModel,
        showImportDialog = false,
        showModelSelectorDialog = false,
        currentScreen = CurrentScreen.CHAT,
        parameters = it.parameters.copy(
          systemPrompt = newModel.defaultSystemPrompt,
          contextWindow = newModel.contextLength,
          backend = if (formatType == ModelFormatType.GGUF) InferenceBackend.CPP_LLAMA else InferenceBackend.RUST_CANDLE
        )
      )
    }
  }

  fun deleteCustomModel(modelId: String) {
    repository.deleteModel(modelId)
    _uiState.update { state ->
      val fallbackModel = if (state.selectedModel?.id == modelId) {
        state.customModels.firstOrNull { it.id != modelId }
      } else {
        state.selectedModel
      }
      state.copy(selectedModel = fallbackModel)
    }
  }

  fun unloadModel() {
    generationJob?.cancel()
    _uiState.update {
      it.copy(
        selectedModel = null,
        currentScreen = CurrentScreen.WELCOME,
        messages = emptyList(),
        isGenerating = false,
        liveTokensPerSec = null
      )
    }
  }

  // Parameters Management
  fun updateParameters(newParameters: InferenceParameters) {
    _uiState.update {
      it.copy(
        parameters = newParameters,
        showParametersDialog = false
      )
    }
  }

  fun resetParameters() {
    val defaultPrompt = _uiState.value.selectedModel?.defaultSystemPrompt
      ?: "Eres un asistente de IA local y privado ejecutado en Android."
    val defaultContext = _uiState.value.selectedModel?.contextLength ?: 4096
    _uiState.update {
      it.copy(
        parameters = InferenceParameters(
          systemPrompt = defaultPrompt,
          contextWindow = defaultContext,
          cpuThreads = it.systemSpecs.availableCores.coerceAtMost(6),
          accelerator = HardwareAccelerator.AUTO,
          useMmap = true
        )
      )
    }
  }

  // Chat & Stream Inferences
  fun sendMessage(userText: String) {
    val currentModel = _uiState.value.selectedModel ?: return
    if (userText.isBlank() || _uiState.value.isGenerating) return

    val currentParams = _uiState.value.parameters
    val currentTokensUsed = _uiState.value.approximateConversationTokens
    val deviceHasNpu = _uiState.value.systemSpecs.hasNpu
    val conversationHistory = _uiState.value.messages

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
        isGenerating = true,
        liveTokensPerSec = null
      )
    }

    generationJob?.cancel()
    generationJob = viewModelScope.launch {
      try {
        engine.generateResponseStream(
          userPrompt = userText,
          model = currentModel,
          parameters = currentParams,
          conversationHistory = conversationHistory,
          estimatedTotalContextTokens = currentTokensUsed,
          deviceHasNpu = deviceHasNpu
        ).collect { chunk ->
          _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
              if (msg.id == assistantMessageId) {
                msg.copy(
                  content = chunk.partialText,
                  isStreaming = !chunk.isFinished,
                  metrics = chunk.metrics ?: msg.metrics,
                  liveTokensPerSec = chunk.liveTokensPerSec,
                  liveHardwareInfo = chunk.liveHardwareInfo
                )
              } else {
                msg
              }
            }
            state.copy(
              messages = updatedMessages,
              isGenerating = !chunk.isFinished,
              liveTokensPerSec = chunk.liveTokensPerSec,
              liveHardwareInfo = chunk.liveHardwareInfo
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
          state.copy(messages = updatedMessages, isGenerating = false, liveTokensPerSec = null)
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
      state.copy(messages = updatedMessages, isGenerating = false, liveTokensPerSec = null)
    }
  }

  fun clearChat() {
    generationJob?.cancel()
    _uiState.update {
      it.copy(
        messages = emptyList(),
        isGenerating = false,
        liveTokensPerSec = null,
        showClearChatDialog = false
      )
    }
  }
}
