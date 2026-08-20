package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.App
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.repository.ChatRepository
import com.example.data.repository.ModelRepository
import com.example.engine.LocalInferenceEngine
import com.example.engine.hardware.HardwareCapabilityDetector
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
  private val chatRepository: ChatRepository = ChatRepository(),
  private val engine: LocalInferenceEngine = LocalInferenceEngine()
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    ChatUiState(
      systemSpecs = HardwareCapabilityDetector.detectSystemSpecs(App.instance?.applicationContext)
    )
  )
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

    // Observe chat sessions
    viewModelScope.launch {
      chatRepository.sessions.collect { sessions ->
        _uiState.update { state ->
          state.copy(chatSessions = sessions)
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

  fun openSafeTensorsImport() {
    _uiState.update {
      it.copy(
        editingSafeTensorsModel = null,
        currentScreen = CurrentScreen.IMPORT_SAFETENSORS
      )
    }
  }

  fun openEditSafeTensors(model: LocalAiModel) {
    _uiState.update {
      it.copy(
        editingSafeTensorsModel = model,
        currentScreen = CurrentScreen.IMPORT_SAFETENSORS
      )
    }
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

  fun showHistoryDialog(show: Boolean) {
    _uiState.update { it.copy(showHistoryDialog = show) }
  }

  // Chat Session Management
  fun startNewChat(targetModel: LocalAiModel? = null) {
    generationJob?.cancel()
    val model = targetModel ?: _uiState.value.selectedModel ?: _uiState.value.customModels.firstOrNull()

    viewModelScope.launch {
      val newSession = chatRepository.createNewSession(
        modelId = model?.id ?: "unknown",
        modelName = model?.name ?: "Sin modelo",
        systemPrompt = model?.defaultSystemPrompt
      )

      val recommendedBackend = when (model?.formatType) {
        ModelFormatType.SAFETENSORS -> InferenceBackend.RUST_CANDLE
        ModelFormatType.TFLITE -> InferenceBackend.TFLITE_RUNTIME
        else -> InferenceBackend.CPP_LLAMA
      }

      _uiState.update { state ->
        val updatedParams = if (model != null) {
          state.parameters.copy(
            systemPrompt = model.defaultSystemPrompt,
            contextWindow = model.contextLength,
            backend = recommendedBackend
          ).sanitize(model, state.systemSpecs.availableCores)
        } else {
          state.parameters
        }

        state.copy(
          currentSessionId = newSession.id,
          currentSessionTitle = newSession.title,
          selectedModel = model ?: state.selectedModel,
          messages = emptyList(),
          isGenerating = false,
          liveTokensPerSec = null,
          showHistoryDialog = false,
          currentScreen = CurrentScreen.CHAT,
          parameters = updatedParams
        )
      }
    }
  }

  fun openChatSession(session: ChatSessionEntity) {
    generationJob?.cancel()
    viewModelScope.launch {
      val correspondingModel = _uiState.value.customModels.find { it.id == session.modelId }
        ?: _uiState.value.selectedModel

      val loadedMessages = chatRepository.loadMessagesForSession(session.id, session.modelName)

      _uiState.update { state ->
        val updatedModel = correspondingModel ?: state.selectedModel
        val updatedParams = if (updatedModel != null) {
          state.parameters.copy(
            systemPrompt = session.systemPrompt ?: updatedModel.defaultSystemPrompt,
            contextWindow = updatedModel.contextLength
          ).sanitize(updatedModel, state.systemSpecs.availableCores)
        } else {
          state.parameters
        }

        state.copy(
          currentSessionId = session.id,
          currentSessionTitle = session.title,
          selectedModel = updatedModel,
          messages = loadedMessages,
          isGenerating = false,
          liveTokensPerSec = null,
          showHistoryDialog = false,
          currentScreen = CurrentScreen.CHAT,
          parameters = updatedParams
        )
      }
    }
  }

  fun deleteChatSession(sessionId: String) {
    viewModelScope.launch {
      chatRepository.deleteSession(sessionId)
      if (_uiState.value.currentSessionId == sessionId) {
        _uiState.update {
          it.copy(
            currentSessionId = null,
            currentSessionTitle = "Nueva conversación",
            messages = emptyList()
          )
        }
      }
    }
  }

  fun renameChatSession(sessionId: String, newTitle: String) {
    if (newTitle.isBlank()) return
    viewModelScope.launch {
      chatRepository.updateSessionTitle(sessionId, newTitle.trim())
      if (_uiState.value.currentSessionId == sessionId) {
        _uiState.update { it.copy(currentSessionTitle = newTitle.trim()) }
      }
    }
  }

  // Model Management & Auto-New-Chat on import
  fun selectModel(model: LocalAiModel) {
    val recommendedBackend = when (model.formatType) {
      ModelFormatType.SAFETENSORS -> InferenceBackend.RUST_CANDLE
      ModelFormatType.TFLITE -> InferenceBackend.TFLITE_RUNTIME
      else -> InferenceBackend.CPP_LLAMA
    }

    _uiState.update {
      val updatedParams = it.parameters.copy(
        systemPrompt = model.defaultSystemPrompt,
        contextWindow = model.contextLength,
        backend = recommendedBackend
      ).sanitize(model, it.systemSpecs.availableCores)

      it.copy(
        selectedModel = model,
        showModelSelectorDialog = false,
        parameters = updatedParams
      )
    }
  }

  fun loadGgufModelDirect(uriOrPath: String, displayName: String? = null) {
    generationJob?.cancel()
    val newModel = repository.addGgufModel(uriOrPath, displayName, App.instance?.applicationContext)

    viewModelScope.launch {
      // Create a fresh session starting from 0 for the newly imported model
      val newSession = chatRepository.createNewSession(
        modelId = newModel.id,
        modelName = newModel.name,
        systemPrompt = newModel.defaultSystemPrompt,
        initialTitle = "Chat con ${newModel.name}"
      )

      _uiState.update {
        val updatedParams = it.parameters.copy(
          systemPrompt = newModel.defaultSystemPrompt,
          contextWindow = newModel.contextLength,
          backend = InferenceBackend.CPP_LLAMA
        ).sanitize(newModel, it.systemSpecs.availableCores)

        it.copy(
          selectedModel = newModel,
          currentSessionId = newSession.id,
          currentSessionTitle = newSession.title,
          messages = emptyList(), // Start from 0!
          isGenerating = false,
          liveTokensPerSec = null,
          currentScreen = CurrentScreen.CHAT,
          parameters = updatedParams
        )
      }
    }
  }

  fun importOrUpdateSafeTensorsBundle(
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
    generationJob?.cancel()
    val currentEditing = _uiState.value.editingSafeTensorsModel

    val resultingModel = if (currentEditing != null) {
      repository.updateSafeTensorsBundle(
        modelId = currentEditing.id,
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
    } else {
      repository.addSafeTensorsBundle(
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
    }

    viewModelScope.launch {
      // If it's a new import or editing, start clean chat session for the model
      val newSession = chatRepository.createNewSession(
        modelId = resultingModel.id,
        modelName = resultingModel.name,
        systemPrompt = resultingModel.defaultSystemPrompt,
        initialTitle = "Chat con ${resultingModel.name}"
      )

      _uiState.update {
        val updatedParams = it.parameters.copy(
          systemPrompt = resultingModel.defaultSystemPrompt,
          contextWindow = resultingModel.contextLength,
          backend = InferenceBackend.RUST_CANDLE
        ).sanitize(resultingModel, it.systemSpecs.availableCores)

        it.copy(
          selectedModel = resultingModel,
          editingSafeTensorsModel = null,
          currentSessionId = newSession.id,
          currentSessionTitle = newSession.title,
          messages = emptyList(), // Start from 0!
          isGenerating = false,
          liveTokensPerSec = null,
          currentScreen = CurrentScreen.CHAT,
          parameters = updatedParams
        )
      }
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
    generationJob?.cancel()
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

    viewModelScope.launch {
      val newSession = chatRepository.createNewSession(
        modelId = newModel.id,
        modelName = newModel.name,
        systemPrompt = newModel.defaultSystemPrompt,
        initialTitle = "Chat con ${newModel.name}"
      )

      _uiState.update {
        val updatedParams = it.parameters.copy(
          systemPrompt = newModel.defaultSystemPrompt,
          contextWindow = newModel.contextLength,
          backend = when (formatType) {
            ModelFormatType.GGUF -> InferenceBackend.CPP_LLAMA
            ModelFormatType.SAFETENSORS -> InferenceBackend.RUST_CANDLE
            ModelFormatType.TFLITE -> InferenceBackend.TFLITE_RUNTIME
          }
        ).sanitize(newModel, it.systemSpecs.availableCores)

        it.copy(
          selectedModel = newModel,
          showImportDialog = false,
          showModelSelectorDialog = false,
          currentSessionId = newSession.id,
          currentSessionTitle = newSession.title,
          messages = emptyList(), // Start from 0!
          isGenerating = false,
          liveTokensPerSec = null,
          currentScreen = CurrentScreen.CHAT,
          parameters = updatedParams
        )
      }
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
      val sanitized = newParameters.sanitize(it.selectedModel, it.systemSpecs.availableCores)
      it.copy(
        parameters = sanitized,
        showParametersDialog = false
      )
    }
  }

  fun resetParameters() {
    val currentModel = _uiState.value.selectedModel
    val defaultPrompt = currentModel?.defaultSystemPrompt
      ?: "Eres un asistente de IA local y privado ejecutado en Android."
    val defaultContext = currentModel?.contextLength ?: 4096
    _uiState.update {
      val defaultParams = InferenceParameters(
        systemPrompt = defaultPrompt,
        contextWindow = defaultContext,
        maxTokens = 512.coerceAtMost(defaultContext),
        cpuThreads = it.systemSpecs.availableCores.coerceAtMost(6),
        accelerator = HardwareAccelerator.AUTO,
        useMmap = true
      ).sanitize(currentModel, it.systemSpecs.availableCores)

      it.copy(parameters = defaultParams)
    }
  }

  // Chat & Stream Inferences with Room persistence
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

    // Ensure session exists
    viewModelScope.launch {
      var sessionId = _uiState.value.currentSessionId
      if (sessionId.isNullOrBlank()) {
        val createdSession = chatRepository.createNewSession(
          modelId = currentModel.id,
          modelName = currentModel.name,
          systemPrompt = currentParams.systemPrompt
        )
        sessionId = createdSession.id
        _uiState.update {
          it.copy(currentSessionId = createdSession.id, currentSessionTitle = createdSession.title)
        }
      }

      // Save user message to database
      chatRepository.saveMessage(sessionId, userMessage, currentModel.name)
    }

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

          if (chunk.isFinished) {
            val sessionId = _uiState.value.currentSessionId
            if (!sessionId.isNullOrBlank()) {
              val finalAssistantMessage = _uiState.value.messages.find { it.id == assistantMessageId }
              if (finalAssistantMessage != null) {
                chatRepository.saveMessage(sessionId, finalAssistantMessage, currentModel.name)
              }
            }
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
    // Save interrupted message to database
    val sessionId = _uiState.value.currentSessionId
    val lastMsg = _uiState.value.messages.lastOrNull()
    if (!sessionId.isNullOrBlank() && lastMsg != null && lastMsg.role == ChatRole.ASSISTANT) {
      viewModelScope.launch {
        chatRepository.saveMessage(sessionId, lastMsg, _uiState.value.selectedModel?.name ?: "")
      }
    }
  }

  fun clearChat() {
    generationJob?.cancel()
    val sessionId = _uiState.value.currentSessionId
    if (!sessionId.isNullOrBlank()) {
      viewModelScope.launch {
        chatRepository.clearSessionMessages(sessionId)
      }
    }
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
