package com.example.engine

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
  val metrics: InferenceMetrics? = null
)

class LocalInferenceEngine {

  fun generateResponseStream(
    userPrompt: String,
    model: LocalAiModel,
    parameters: InferenceParameters
  ): Flow<StreamChunk> = flow {
    val startTime = System.currentTimeMillis()

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

    // Simulate initial model context ingestion & KV-cache allocation
    val contextDelay = when (model.parameterSize) {
      "3.8B" -> 220L
      "2.6B" -> 160L
      "1.5B" -> 110L
      else -> 75L
    }
    delay(contextDelay)

    val fullResponse = produceResponseText(userPrompt, model, parameters)
    val words = fullResponse.split(" ")
    val accumulated = StringBuilder()

    // Calculate delay per token based on model size, CPU threads, and backend efficiency
    val baseMsPerToken = when (model.parameterSize) {
      "3.8B" -> 55L
      "2.6B" -> 40L
      "1.5B" -> 30L
      else -> 22L
    }

    // C++ and Rust give 20-30% native speedup advantage over pure bytecode
    val backendMultiplier = when (parameters.backend) {
      InferenceBackend.CPP_LLAMA -> 0.75f
      InferenceBackend.RUST_CANDLE -> 0.80f
      InferenceBackend.KOTLIN_RUNTIME -> 1.0f
    }

    val threadModifier = (1.0f - ((parameters.cpuThreads.coerceIn(1, 8) - 1) * 0.07f)).coerceAtLeast(0.5f)
    val msPerToken = (baseMsPerToken * threadModifier * backendMultiplier).toLong().coerceAtLeast(10L)

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

      val jitter = Random.nextLong(-3, 4)
      delay((msPerToken + jitter).coerceAtLeast(8L))

      emit(StreamChunk(partialText = accumulated.toString(), isFinished = false))
    }

    val totalTimeMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
    val finalTokens = tokenCount.coerceAtLeast(1)
    val tokensPerSec = (finalTokens.toDouble() / (totalTimeMs / 1000.0)).let {
      (it * 10).roundToInt() / 10.0
    }

    val estimatedRamMb = when (model.parameterSize) {
      "3.8B" -> 2240
      "2.6B" -> 1580
      "1.5B" -> 1120
      else -> 640
    }

    val metrics = InferenceMetrics(
      modelName = model.name,
      backendName = parameters.backend.displayName,
      tokensGenerated = finalTokens,
      generationTimeMs = totalTimeMs,
      tokensPerSecond = tokensPerSec,
      ramUsageMb = estimatedRamMb,
      temperature = parameters.temperature
    )

    emit(StreamChunk(partialText = accumulated.toString(), isFinished = true, metrics = metrics))
  }

  private fun produceResponseText(
    prompt: String,
    model: LocalAiModel,
    params: InferenceParameters
  ): String {
    val cleanPrompt = prompt.trim().lowercase()

    return when {
      cleanPrompt.contains("hola") || cleanPrompt.contains("buenas") || cleanPrompt == "hi" -> {
        "¡Hola! Soy **${model.name}**, tu modelo de lenguaje ejecutándose localmente en este dispositivo Android con el motor **${params.backend.displayName}**.\n\n" +
        "🔒 **Privacidad total:** Todo nuestro procesamiento ocurre 100% en la memoria de tu teléfono, sin enviar ningún dato a servidores externos ni requerir conexión a internet.\n\n" +
        "¿En qué puedo ayudarte hoy?"
      }

      cleanPrompt.contains("c++") || cleanPrompt.contains("rust") || cleanPrompt.contains("ndk") || cleanPrompt.contains("motor") -> {
        "⚙️ **Arquitectura de motores nativos configurada:**\n\n" +
        "• **Motor C++ (`llama.cpp` / JNI):** Permite vectorización SIMD con **ARM NEON** y aceleración en GPU móvil por Vulkan.\n" +
        "• **Motor Rust (`Candle` / UniFFI):** Implementa tensores seguros con cero sobrecarga y verificación estricta de memoria.\n" +
        "• **Motor activo actual:** **${params.backend.displayName}** (${params.backend.techDescription})."
      }

      cleanPrompt.contains("quien eres") || cleanPrompt.contains("quién eres") || cleanPrompt.contains("modelo") -> {
        "Soy **${model.name}** (${model.parameterSize} parámetros, cuantización ${model.quantization}), desarrollado por **${model.developer}**.\n\n" +
        "• **Motor nativo:** ${params.backend.displayName} (${params.cpuThreads} hilos CPU)\n" +
        "• **Memoria asignada:** ${model.ramRequired}\n" +
        "• **Temperatura:** ${params.temperature} (Nivel de creatividad)\n" +
        "• **Ventana de contexto:** ${params.contextWindow} tokens\n\n" +
        "Estoy optimizado para: *${model.recommendedFor}*."
      }

      cleanPrompt.contains("offline") || cleanPrompt.contains("local") || cleanPrompt.contains("internet") || cleanPrompt.contains("privacidad") -> {
        "Ejecutar modelos de IA localmente en Android ofrece ventajas fundamentales:\n\n" +
        "1. **Privacidad absoluta:** Tus preguntas, documentos e información personal jamás abandonan tu teléfono.\n" +
        "2. **Cero costos de API:** No hay suscripciones ni pagos por tokens procesados.\n" +
        "3. **Disponibilidad continua:** Funciona en modo avión, zonas sin cobertura o viajes.\n" +
        "4. **Motor de inferencia nativo:** Impulsado por **${params.backend.displayName}** para máximo rendimiento en tu batería."
      }

      cleanPrompt.contains("gguf") || cleanPrompt.contains("cuantizacion") || cleanPrompt.contains("cuantización") -> {
        "**GGUF** (GPT-Generated Unified Format) es el formato estándar para almacenar y ejecutar modelos de lenguaje en procesadores de consumo y dispositivos móviles como Android:\n\n" +
        "• **¿Qué es la cuantización?** Reduce el peso de los pesos del modelo (de 16 bits a 4 u 8 bits por parámetro).\n" +
        "• **Tu cuantización actual (${model.quantization}):** Logra un balance ideal entre velocidad de inferencia y coherencia del lenguaje, permitiendo que un modelo de ${model.parameterSize} quepa en ${model.ramRequired}."
      }

      cleanPrompt.contains("parametro") || cleanPrompt.contains("parámetro") || cleanPrompt.contains("temperatura") -> {
        "Los parámetros configurados en tu sesión actual son:\n\n" +
        "• **Motor:** ${params.backend.displayName}\n" +
        "• **Temperatura (${params.temperature}):** Controla la aleatoriedad. Valores bajos (0.1 - 0.3) son exactos; valores altos (0.8 - 1.2) son creativos.\n" +
        "• **Top-P (${params.topP}):** Muestreo por núcleo.\n" +
        "• **Hilos de CPU (${params.cpuThreads}):** Número de núcleos dedicados al cálculo.\n" +
        "• **Repeat Penalty (${params.repeatPenalty}):** Penaliza repeticiones."
      }

      cleanPrompt.contains("codigo") || cleanPrompt.contains("código") || cleanPrompt.contains("kotlin") || cleanPrompt.contains("python") -> {
        "Aquí tienes la integración del puente JNI entre Kotlin y C++/Rust para la inferencia local:\n\n" +
        "```kotlin\n" +
        "// Invocación del motor nativo en Android\n" +
        "suspend fun executeNativeInference(\n" +
        "    prompt: String,\n" +
        "    backend: InferenceBackend = InferenceBackend.${params.backend.name}\n" +
        "): Flow<String> = flow {\n" +
        "    val handle = NativeCppBridge.initModelContextNative(\"${model.id}\", ${params.cpuThreads}, ${params.contextWindow})\n" +
        "    val result = NativeCppBridge.evaluatePromptNative(handle, prompt, ${params.temperature}f, ${params.topP}f, ${params.maxTokens})\n" +
        "    emit(result)\n" +
        "    NativeCppBridge.freeModelContextNative(handle)\n" +
        "}\n" +
        "```"
      }

      else -> {
        "Entendido. Procesando tu solicitud con **${model.name}** a través del motor nativo **${params.backend.displayName}**:\n\n" +
        "Respecto a *\"$prompt\"*:\n\n" +
        "1. **Seguridad y privacidad:** El procesamiento se ha ejecutado directamente en los núcleos de CPU de tu teléfono (${params.cpuThreads} hilos).\n" +
        "2. **Respuesta local:** Generada con temperatura **${params.temperature}** y ventana de contexto de **${params.contextWindow} tokens**.\n" +
        "3. **Ajustes:** Puedes alternar el motor de inferencia entre **C++**, **Rust** o **Kotlin** desde el menú de parámetros en cualquier momento."
      }
    }
  }
}
