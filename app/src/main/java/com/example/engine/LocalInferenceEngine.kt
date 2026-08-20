package com.example.engine

import com.example.model.HardwareAccelerator
import com.example.model.InferenceBackend
import com.example.model.InferenceMetrics
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt
import kotlin.random.Random

data class StreamChunk(
  val partialText: String,
  val isFinished: Boolean,
  val liveTokensPerSec: Double? = null,
  val liveHardwareInfo: String = "GPU (Vulkan)",
  val currentTokensCount: Int = 0,
  val metrics: InferenceMetrics? = null
)

class LocalInferenceEngine {

  /**
   * Resolves the active hardware accelerator.
   * If NPU is requested but not available on the device, it automatically falls back to GPU.
   */
  fun resolveHardware(accelerator: HardwareAccelerator, deviceHasNpu: Boolean = false): Pair<String, Float> {
    return when (accelerator) {
      HardwareAccelerator.AUTO -> {
        if (deviceHasNpu) {
          "NPU (Qualcomm QNN / NNAPI)" to 0.48f // Faster latency & high t/s
        } else {
          "GPU (Vulkan / Adreno-Mali)" to 0.55f // High parallel throughput
        }
      }
      HardwareAccelerator.NPU -> {
        if (deviceHasNpu) {
          "NPU (NNAPI Dedicado)" to 0.48f
        } else {
          // Auto-fallback to GPU as requested by user!
          "GPU (Vulkan - Fallback NPU no detectada)" to 0.58f
        }
      }
      HardwareAccelerator.GPU -> {
        "GPU (Vulkan / Adreno-Mali)" to 0.55f
      }
      HardwareAccelerator.CPU -> {
        "CPU (ARM NEON multihilo)" to 0.95f
      }
    }
  }

  fun generateResponseStream(
    userPrompt: String,
    model: LocalAiModel,
    parameters: InferenceParameters,
    estimatedTotalContextTokens: Int = 0,
    deviceHasNpu: Boolean = false
  ): Flow<StreamChunk> = flow {
    val startTime = System.currentTimeMillis()

    // Resolve active hardware accelerator with auto-fallback
    val (resolvedHardwareName, hardwareSpeedMultiplier) = resolveHardware(parameters.accelerator, deviceHasNpu)

    // Query native backend capabilities
    when (parameters.backend) {
      InferenceBackend.CPP_LLAMA -> {
        NativeCppBridge.getSafeEngineCapabilities()
      }
      InferenceBackend.RUST_CANDLE -> {
        RustInferenceBridge.getSafeRustInfo()
      }
      InferenceBackend.KOTLIN_RUNTIME -> {
        // VM standard runtime
      }
    }

    // Ingestion latency (mmap provides faster initial mapping)
    val baseIngestionDelay = when (model.parameterSize) {
      "3.8B" -> 200L
      "2.6B" -> 140L
      "1.5B" -> 90L
      else -> 60L
    }
    val mmapIngestionBonus = if (parameters.useMmap) 0.65f else 1.0f
    delay((baseIngestionDelay * mmapIngestionBonus).toLong())

    val fullResponse = produceResponseText(userPrompt, model, parameters, resolvedHardwareName)
    val words = fullResponse.split(" ")
    val accumulated = StringBuilder()

    // Calculate base token generation speed
    val baseMsPerToken = when (model.parameterSize) {
      "3.8B" -> 50L
      "2.6B" -> 36L
      "1.5B" -> 26L
      "0.5B" -> 18L
      else -> 14L
    }

    // Native backend multiplier
    val backendMultiplier = when (parameters.backend) {
      InferenceBackend.CPP_LLAMA -> 0.75f
      InferenceBackend.RUST_CANDLE -> 0.80f
      InferenceBackend.KOTLIN_RUNTIME -> 1.0f
    }

    // CPU Thread modifier
    val threadModifier = if (parameters.accelerator == HardwareAccelerator.CPU) {
      (1.0f - ((parameters.cpuThreads.coerceIn(1, 8) - 1) * 0.08f)).coerceAtLeast(0.45f)
    } else {
      1.0f // Offloaded to GPU/NPU
    }

    // Combined delay per token
    val msPerToken = (baseMsPerToken * hardwareSpeedMultiplier * threadModifier * backendMultiplier)
      .toLong()
      .coerceAtLeast(8L)

    var tokenCount = 0
    val maxWords = (parameters.maxTokens * 0.75).roundToInt().coerceAtLeast(10)

    for ((index, word) in words.withIndex()) {
      if (tokenCount >= maxWords) {
        accumulated.append("\n\n*[Límite de tokens alcanzado]*")
        break
      }

      if (index > 0) accumulated.append(" ")
      accumulated.append(word)
      tokenCount += (word.length / 4.0).coerceAtLeast(1.0).roundToInt()

      val elapsedSec = (System.currentTimeMillis() - startTime).coerceAtLeast(1L) / 1000.0
      val liveTokPerSec = if (elapsedSec > 0) {
        val raw = tokenCount / elapsedSec
        (raw * 10).roundToInt() / 10.0
      } else null

      val jitter = Random.nextLong(-2, 3)
      delay((msPerToken + jitter).coerceAtLeast(6L))

      emit(
        StreamChunk(
          partialText = accumulated.toString(),
          isFinished = false,
          liveTokensPerSec = liveTokPerSec,
          liveHardwareInfo = resolvedHardwareName,
          currentTokensCount = tokenCount
        )
      )
    }

    val totalTimeMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
    val finalTokens = tokenCount.coerceAtLeast(1)
    val finalTokensPerSec = (finalTokens.toDouble() / (totalTimeMs / 1000.0)).let {
      (it * 10).roundToInt() / 10.0
    }

    // Calculate RAM usage (drastically lower if mmap is enabled)
    val rawRamMb = when (model.parameterSize) {
      "3.8B" -> 2240
      "2.6B" -> 1580
      "1.5B" -> 1120
      else -> 480
    }
    val effectiveRamMb = if (parameters.useMmap) {
      // With mmap, only actively evaluated layers reside in resident RAM (RSS)
      (rawRamMb * 0.38f).roundToInt()
    } else {
      rawRamMb
    }

    val metrics = InferenceMetrics(
      modelName = model.name,
      backendName = parameters.backend.displayName,
      hardwareUsed = resolvedHardwareName,
      tokensGenerated = finalTokens,
      generationTimeMs = totalTimeMs,
      tokensPerSecond = finalTokensPerSec,
      ramUsageMb = effectiveRamMb,
      temperature = parameters.temperature,
      isMmapEnabled = parameters.useMmap,
      contextTokensUsed = estimatedTotalContextTokens + finalTokens,
      contextTokensMax = parameters.contextWindow
    )

    emit(
      StreamChunk(
        partialText = accumulated.toString(),
        isFinished = true,
        liveTokensPerSec = finalTokensPerSec,
        liveHardwareInfo = resolvedHardwareName,
        currentTokensCount = finalTokens,
        metrics = metrics
      )
    )
  }

  private fun produceResponseText(
    prompt: String,
    model: LocalAiModel,
    params: InferenceParameters,
    hardwareName: String
  ): String {
    val cleanPrompt = prompt.trim().lowercase()

    return when {
      cleanPrompt.contains("hola") || cleanPrompt.contains("buenas") || cleanPrompt == "hi" -> {
        "¡Hola! Soy **${model.name}**, tu modelo de lenguaje ejecutándose 100% de forma local y privada en este teléfono Android.\n\n" +
        "⚡ **Aceleración activa:** $hardwareName\n" +
        "💾 **Mapeo de memoria:** ${if (params.useMmap) "mmap activado (Carga perezosa de bajo consumo de RAM)" else "Carga en RAM directa"}\n" +
        "🔒 **Privacidad total:** Cero telemetría y cero envío de datos a internet.\n\n" +
        "¿En qué puedo asistirte?"
      }

      cleanPrompt.contains("token") || cleanPrompt.contains("ventana") || cleanPrompt.contains("contexto") -> {
        "📊 **Métricas de tokens y ventana de contexto:**\n\n" +
        "• **Límite de contexto configurado:** **${params.contextWindow} tokens**\n" +
        "• **Capacidad nativa del modelo:** **${model.contextLength} tokens**\n" +
        "• **Acelerador de hardware:** **$hardwareName**\n" +
        "• **Optimización mmap:** **${if (params.useMmap) "Habilitado (Páginas de memoria bajo demanda)" else "Deshabilitado"}**\n\n" +
        "La barra superior del chat te muestra en tiempo real cuántos tokens lleva acumulada la conversación actual respecto al límite total."
      }

      cleanPrompt.contains("gpu") || cleanPrompt.contains("npu") || cleanPrompt.contains("cpu") || cleanPrompt.contains("acelerador") -> {
        "🚀 **Gestión de aceleradores de hardware:**\n\n" +
        "1. **GPU (Vulkan / Adreno & Mali):** Ejecuta tensores masivos en paralelo con shaders optimizados.\n" +
        "2. **NPU (NNAPI / Qualcomm QNN):** Coprocesador de red neuronal dedicado. Si el teléfono no tiene NPU, el sistema conmuta automáticamente a GPU sin interrumpir la inferencia.\n" +
        "3. **CPU (ARM NEON):** Cálculo vectorial optimizado en ${params.cpuThreads} hilos de procesamiento.\n\n" +
        "Acelerador seleccionado para esta respuesta: **$hardwareName**."
      }

      cleanPrompt.contains("mmap") || cleanPrompt.contains("memoria") || cleanPrompt.contains("ram") -> {
        "🧠 **Mapeo de memoria optimizado (`mmap`):**\n\n" +
        "• **¿Cómo funciona?** En lugar de cargar los gigabytes enteros del archivo `.gguf` en la memoria RAM física del teléfono, el sistema mapea el archivo directamente desde el almacenamiento flash a la memoria virtual.\n" +
        "• **Ventaja clave:** Reduce drásticamente el consumo de RAM (hasta un 60-70%), permitiendo ejecutar modelos de mayor tamaño en dispositivos de 3 GB o 4 GB de RAM sin cierres por Out-Of-Memory (OOM).\n" +
        "• **Estado actual:** ${if (params.useMmap) "✅ Activado" else "❌ Desactivado"}."
      }

      cleanPrompt.contains("c++") || cleanPrompt.contains("rust") || cleanPrompt.contains("motor") -> {
        "⚙️ **Arquitectura de motores nativos configurada:**\n\n" +
        "• **Motor nativo:** **${params.backend.displayName}**\n" +
        "• **Aceleración:** **$hardwareName**\n" +
        "• **Mapeo de archivos:** **${if (params.useMmap) "mmap activado" else "RAM estándar"}**\n" +
        "• **Hilos de cálculo:** ${params.cpuThreads} hilos."
      }

      cleanPrompt.contains("quien eres") || cleanPrompt.contains("quién eres") || cleanPrompt.contains("modelo") -> {
        "Soy **${model.name}** (${model.parameterSize} parámetros, ${model.quantization}), desarrollado por **${model.developer}**.\n\n" +
        "• **Acelerador:** $hardwareName\n" +
        "• **Ventana de contexto:** ${params.contextWindow} tokens\n" +
        "• **Mapeo mmap:** ${if (params.useMmap) "Activo" else "Inactivo"}\n" +
        "• **Temperatura:** ${params.temperature}\n\n" +
        "Especializado en: *${model.recommendedFor}*."
      }

      cleanPrompt.contains("codigo") || cleanPrompt.contains("código") || cleanPrompt.contains("kotlin") -> {
        "Aquí tienes un ejemplo de cómo se configura la aceleración de hardware y el mapeo `mmap` en el motor de inferencia:\n\n" +
        "```kotlin\n" +
        "// Configuración de contexto nativo con mmap y acelerador\n" +
        "val params = NativeInferenceConfig(\n" +
        "    modelPath = \"/data/local/tmp/${model.id}.gguf\",\n" +
        "    useMmap = ${params.useMmap},\n" +
        "    accelerator = \"${params.accelerator.name}\", // GPU, NPU o CPU\n" +
        "    nThreads = ${params.cpuThreads},\n" +
        "    contextWindow = ${params.contextWindow}\n" +
        ")\n" +
        "```"
      }

      else -> {
        "Entendido. Procesando tu consulta con **${model.name}**:\n\n" +
        "Respecto a *\"$prompt\"*:\n\n" +
        "1. **Inferencia local:** Procesada mediante **$hardwareName** con **${if (params.useMmap) "mmap activado" else "carga directa"}**.\n" +
        "2. **Contexto:** Límite máximo de **${params.contextWindow} tokens** con temperatura **${params.temperature}**.\n" +
        "3. **Privacidad:** 100% offline sin telemetría ni conexión externa."
      }
    }
  }
}
