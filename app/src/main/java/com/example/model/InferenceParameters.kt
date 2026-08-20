package com.example.model

enum class HardwareAccelerator(
  val displayName: String,
  val techDescription: String,
  val badge: String,
  val isNpu: Boolean = false
) {
  AUTO(
    displayName = "Automático (NPU / GPU Fallback)",
    techDescription = "Prioriza NPU si el hardware cuenta con él; si no, conmuta automáticamente a GPU (Vulkan)",
    badge = "Auto"
  ),
  GPU(
    displayName = "GPU (Vulkan / Adreno-Mali)",
    techDescription = "Aceleración gráfica de alto paralelismo con tensores Vulkan / OpenCL",
    badge = "GPU"
  ),
  NPU(
    displayName = "NPU (NNAPI / Qualcomm QNN)",
    techDescription = "Coprocesador de red neuronal dedicado de ultra bajo consumo (con fallback a GPU)",
    badge = "NPU",
    isNpu = true
  ),
  CPU(
    displayName = "CPU (ARM NEON Multihilo)",
    techDescription = "Procesamiento vectorial estándar en los núcleos del procesador",
    badge = "CPU"
  )
}

enum class InferenceBackend(
  val displayName: String,
  val techDescription: String,
  val badge: String
) {
  CPP_LLAMA(
    displayName = "C++ (llama.cpp / NDK)",
    techDescription = "Máxima velocidad nativa con aceleración ARM NEON, Vulkan y mmap",
    badge = "C++ 17"
  ),
  RUST_CANDLE(
    displayName = "Rust (Candle / UniFFI)",
    techDescription = "Inferencia con seguridad de memoria estricta y concurrencia segura",
    badge = "Rust 2021"
  ),
  TFLITE_RUNTIME(
    displayName = "TensorFlow Lite (TFLite / LiteRT)",
    techDescription = "Inferencia móvil con delegados GPU (OpenCL/OpenGL) y NNAPI NPU",
    badge = "TFLite"
  ),
  KOTLIN_RUNTIME(
    displayName = "Kotlin Runtime",
    techDescription = "Motor integrado en máquina virtual con corrutinas asíncronas",
    badge = "Kotlin"
  )
}

data class InferenceParameters(
  val backend: InferenceBackend = InferenceBackend.CPP_LLAMA,
  val accelerator: HardwareAccelerator = HardwareAccelerator.AUTO,
  val useMmap: Boolean = true,
  val temperature: Float = 0.7f,
  val topP: Float = 0.90f,
  val topK: Int = 40,
  val maxTokens: Int = 512,
  val repeatPenalty: Float = 1.15f,
  val cpuThreads: Int = 4,
  val contextWindow: Int = 4096,
  val systemPrompt: String = "Eres un asistente de IA inteligente, empático y 100% privado ejecutado localmente en este dispositivo Android."
) {
  companion object {
    const val MIN_CONTEXT_WINDOW = 256
    const val DEFAULT_MAX_CONTEXT_WINDOW = 8192
    const val MIN_MAX_TOKENS = 16
    const val MIN_TEMPERATURE = 0.0f
    const val MAX_TEMPERATURE = 2.0f
    const val MIN_TOP_P = 0.01f
    const val MAX_TOP_P = 1.0f
    const val MIN_TOP_K = 1
    const val MAX_TOP_K = 100
    const val MIN_REPEAT_PENALTY = 1.0f
    const val MAX_REPEAT_PENALTY = 2.0f
  }

  /**
   * Clamps and validates parameters against the active model's architecture limits
   * and safe operational ranges without risking model degradation or crashes.
   */
  fun sanitize(
    model: LocalAiModel? = null,
    maxAvailableCores: Int = 8
  ): InferenceParameters {
    val modelMaxContext = model?.contextLength?.coerceAtLeast(MIN_CONTEXT_WINDOW) ?: DEFAULT_MAX_CONTEXT_WINDOW
    val safeContextWindow = contextWindow.coerceIn(MIN_CONTEXT_WINDOW, modelMaxContext)
    val safeMaxTokens = maxTokens.coerceIn(MIN_MAX_TOKENS, safeContextWindow)
    val safeTemp = temperature.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE)
    val safeTopP = topP.coerceIn(MIN_TOP_P, MAX_TOP_P)
    val safeTopK = topK.coerceIn(MIN_TOP_K, MAX_TOP_K)
    val safeRepeatPenalty = repeatPenalty.coerceIn(MIN_REPEAT_PENALTY, MAX_REPEAT_PENALTY)
    val safeCpuThreads = cpuThreads.coerceIn(1, maxAvailableCores.coerceAtLeast(1))

    return this.copy(
      contextWindow = safeContextWindow,
      maxTokens = safeMaxTokens,
      temperature = safeTemp,
      topP = safeTopP,
      topK = safeTopK,
      repeatPenalty = safeRepeatPenalty,
      cpuThreads = safeCpuThreads
    )
  }
}
