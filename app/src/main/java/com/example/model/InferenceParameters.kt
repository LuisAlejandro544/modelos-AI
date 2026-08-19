package com.example.model

enum class InferenceBackend(
  val displayName: String,
  val techDescription: String,
  val badge: String
) {
  CPP_LLAMA(
    displayName = "C++ (llama.cpp / NDK)",
    techDescription = "Máxima velocidad nativa con aceleración ARM NEON y Vulkan",
    badge = "C++ 17"
  ),
  RUST_CANDLE(
    displayName = "Rust (Candle / UniFFI)",
    techDescription = "Inferencia con seguridad de memoria estricta y concurrencia segura",
    badge = "Rust 2021"
  ),
  KOTLIN_RUNTIME(
    displayName = "Kotlin Runtime",
    techDescription = "Motor integrado en máquina virtual con corrutinas asíncronas",
    badge = "Kotlin"
  )
}

data class InferenceParameters(
  val backend: InferenceBackend = InferenceBackend.CPP_LLAMA,
  val temperature: Float = 0.7f,
  val topP: Float = 0.90f,
  val topK: Int = 40,
  val maxTokens: Int = 512,
  val repeatPenalty: Float = 1.15f,
  val cpuThreads: Int = 4,
  val contextWindow: Int = 2048,
  val systemPrompt: String = "Eres un asistente de IA inteligente, empático y 100% privado ejecutado localmente en este dispositivo Android."
)
