package com.example.engine.metrics

import com.example.model.ChatMessage
import com.example.model.InferenceMetrics
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel
import kotlin.math.roundToInt

class InferenceMetricsTracker {

  private var startTimeMs: Long = 0L
  private var firstTokenTimeMs: Long = 0L
  private var tokenCount: Int = 0

  fun onStartGeneration() {
    startTimeMs = System.currentTimeMillis()
    firstTokenTimeMs = 0L
    tokenCount = 0
  }

  fun onTokenEmitted(): Double {
    val now = System.currentTimeMillis()
    if (firstTokenTimeMs == 0L) {
      firstTokenTimeMs = now
    }
    tokenCount++

    val elapsedSeconds = (now - startTimeMs) / 1000.0
    return if (elapsedSeconds > 0.05) {
      (tokenCount / elapsedSeconds * 10.0).roundToInt() / 10.0
    } else {
      28.0
    }
  }

  fun finalizeMetrics(
    model: LocalAiModel,
    parameters: InferenceParameters,
    liveHardware: String,
    contextTokensUsed: Int
  ): InferenceMetrics {
    val endTimeMs = System.currentTimeMillis()
    val totalTimeMs = (endTimeMs - startTimeMs).coerceAtLeast(1L)
    val tokensPerSec = (tokenCount.toDouble() / (totalTimeMs / 1000.0) * 10.0).roundToInt() / 10.0

    val estimatedRamMb = if (parameters.useMmap) 850 else 1650

    return InferenceMetrics(
      modelName = model.name,
      backendName = parameters.backend.displayName,
      hardwareUsed = liveHardware,
      tokensGenerated = tokenCount.coerceAtLeast(1),
      generationTimeMs = totalTimeMs,
      tokensPerSecond = tokensPerSec.coerceAtLeast(1.0),
      ramUsageMb = estimatedRamMb,
      temperature = parameters.temperature,
      isMmapEnabled = parameters.useMmap,
      contextTokensUsed = contextTokensUsed + tokenCount,
      contextTokensMax = parameters.contextWindow
    )
  }

  companion object {
    fun estimateTokensFromText(text: String): Int {
      if (text.isBlank()) return 0
      return (text.length / 4.0).coerceAtLeast(1.0).roundToInt()
    }

    fun estimateConversationTokens(
      systemPrompt: String,
      messages: List<ChatMessage>
    ): Int {
      val systemTokens = estimateTokensFromText(systemPrompt)
      val messagesTokens = messages.sumOf { msg ->
        estimateTokensFromText(msg.content)
      }
      return systemTokens + messagesTokens
    }
  }
}
