package com.example.model

enum class ChatRole {
  USER,
  ASSISTANT,
  SYSTEM
}

data class InferenceMetrics(
  val modelName: String,
  val backendName: String = "C++ llama.cpp",
  val hardwareUsed: String = "GPU (Vulkan)",
  val tokensGenerated: Int,
  val generationTimeMs: Long,
  val tokensPerSecond: Double,
  val ramUsageMb: Int,
  val temperature: Float,
  val isMmapEnabled: Boolean = true,
  val contextTokensUsed: Int = 0,
  val contextTokensMax: Int = 4096
)

data class ChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val role: ChatRole,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val metrics: InferenceMetrics? = null,
  val isStreaming: Boolean = false,
  val liveTokensPerSec: Double? = null,
  val liveHardwareInfo: String? = null
)
