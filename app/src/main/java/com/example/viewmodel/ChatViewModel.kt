package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlin.math.roundToInt

enum class CurrentScreen {
  WELCOME,
  IMPORT_SAFETENSORS,
  CHAT
}

data class SystemSpecs(
  val availableCores: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
  val totalMemoryMb: Long = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).coerceAtLeast(1024),
  val isOfflineModeActive: Boolean = true,
  val hasGpuVulkan: Boolean = true,
  val hasNpu: Boolean = false, // Phone without dedicated NPU -> auto-fallback to GPU
  val storageUsedFormatted: String = "2.4 GB libres"
)

data class ChatUiState(
  val currentScreen: CurrentScreen = CurrentScreen.WELCOME,
  val selectedModel: LocalAiModel? = null,
  val customModels: List<LocalAiModel> = emptyList(),
  val parameters: InferenceParameters = InferenceParameters(),
  val messages: List<ChatMessage> = emptyList(),
  val isGenerating: Boolean = false,
  val liveTokensPerSec: Double? = null,
  val liveHardwareInfo: String = "GPU (Vulkan)",
  val showModelSelectorDialog: Boolean = false,
  val showImportDialog: Boolean = false,
  val showParametersDialog: Boolean = false,
  val showClearChatDialog: Boolean = false,
  val showTokenizerGuideDialog: Boolean = false,
  val systemSpecs: SystemSpecs = SystemSpecs()
) {
  val allAvailableModels: List<LocalAiModel>
    get() = customModels

  /**
   * Approximate tokens in the entire conversation plus system prompt
   */
  val approximateConversationTokens: Int
    get() {
      val systemTokens = (parameters.systemPrompt.length / 4.0).roundToInt()
      val messagesTokens = messages.sumOf { msg ->
        (msg.content.length / 4.0).coerceAtLeast(1.0).roundToInt()
      }
      return systemTokens + messagesTokens
    }

  val contextLimit: Int
    get() = parameters.contextWindow.coerceAtMost(selectedModel?.contextLength ?: 4096)

  val contextUsagePercentage: Float
    get() = (approximateConversationTokens.toFloat() / contextLimit.coerceAtLeast(1) * 100f).coerceIn(0f, 100f)
}

class ChatViewModel(
  private val engine: LocalInferenceEngine = LocalInferenceEngine()
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private var generationJob: Job? = null

  init {
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
          contextWindow = model.contextLength,
          backend = recommendedBackend
        )
      )
    }
  }

  fun loadGgufModelDirect(uriOrPath: String, displayName: String? = null) {
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

    _uiState.update {
      it.copy(
        customModels = listOf(newModel) + it.customModels,
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

    _uiState.update {
      it.copy(
        customModels = listOf(newModel) + it.customModels,
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

  fun deleteCustomModel(modelId: String) {
    _uiState.update { state ->
      val updatedCustom = state.customModels.filterNot { it.id == modelId }
      val fallbackModel = if (state.selectedModel?.id == modelId) {
        updatedCustom.firstOrNull()
      } else {
        state.selectedModel
      }
      state.copy(
        customModels = updatedCustom,
        selectedModel = fallbackModel
      )
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

  fun sendMessage(userText: String) {
    val currentModel = _uiState.value.selectedModel ?: return
    if (userText.isBlank() || _uiState.value.isGenerating) return

    val currentParams = _uiState.value.parameters
    val currentTokensUsed = _uiState.value.approximateConversationTokens
    val deviceHasNpu = _uiState.value.systemSpecs.hasNpu

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

    _uiState.update {
      it.copy(
        customModels = listOf(newModel) + it.customModels,
        selectedModel = newModel,
        showImportDialog = false,
        showModelSelectorDialog = false,
        currentScreen = CurrentScreen.CHAT,
        parameters = it.parameters.copy(
          systemPrompt = newModel.defaultSystemPrompt,
          contextWindow = newModel.contextLength,
          backend = if (isGguf) InferenceBackend.CPP_LLAMA else InferenceBackend.RUST_CANDLE
        )
      )
    }
  }

  fun showImportDialog(show: Boolean) {
    _uiState.update { it.copy(showImportDialog = show) }
  }
}

