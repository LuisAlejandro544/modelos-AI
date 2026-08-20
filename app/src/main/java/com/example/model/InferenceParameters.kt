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
)
