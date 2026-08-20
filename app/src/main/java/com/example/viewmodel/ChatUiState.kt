package com.example.viewmodel

import com.example.engine.hardware.SystemSpecs as EngineSystemSpecs
import com.example.engine.metrics.InferenceMetricsTracker
import com.example.model.ChatMessage
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel

typealias SystemSpecs = EngineSystemSpecs

enum class CurrentScreen {
  WELCOME,
  IMPORT_SAFETENSORS,
  CHAT
}

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

  val approximateConversationTokens: Int
    get() = InferenceMetricsTracker.estimateConversationTokens(
      systemPrompt = parameters.systemPrompt,
      messages = messages
    )

  val contextLimit: Int
    get() = parameters.contextWindow.coerceAtMost(selectedModel?.contextLength ?: 4096)

  val contextUsagePercentage: Float
    get() = (approximateConversationTokens.toFloat() / contextLimit.coerceAtLeast(1) * 100f).coerceIn(0f, 100f)
}
