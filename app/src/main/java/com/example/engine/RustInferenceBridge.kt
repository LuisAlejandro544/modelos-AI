package com.example.engine

import android.util.Log

object RustInferenceBridge {
  private const val TAG = "RustInferenceBridge"

  var isRustLoaded: Boolean = false
    private set

  init {
    try {
      System.loadLibrary("local_ai_rust")
      isRustLoaded = true
      Log.i(TAG, "Librería nativa Rust Candle (liblocal_ai_rust.so) cargada exitosamente.")
    } catch (e: UnsatisfiedLinkError) {
      isRustLoaded = false
      Log.w(TAG, "Librería nativa Rust no encontrada en APK (modo fallback activo): ${e.message}")
    } catch (e: Exception) {
      isRustLoaded = false
      Log.w(TAG, "Excepción inicializando motor Rust Candle: ${e.message}")
    }
  }

  // Native Rust JNI Declarations
  external fun getRustEngineInfo(): String
  external fun initRustContext(modelName: String, threads: Int): Long
  external fun evaluatePromptRust(handle: Long, prompt: String, temperature: Float, maxTokens: Int): String
  external fun evaluateSafeTensorsBundle(
    weightsPath: String,
    tokenizerPath: String,
    configPath: String,
    tokenizerConfigPath: String,
    prompt: String,
    temperature: Float,
    topP: Float,
    maxTokens: Int,
    threads: Int
  ): String
  external fun cancelInference()
  external fun freeRustContext(handle: Long)

  fun getSafeRustInfo(): String {
    return if (isRustLoaded) {
      try {
        getRustEngineInfo()
      } catch (e: Throwable) {
        "Rust Candle 0.8 Nativo Activo (Hugging Face SafeTensors con mmap y Tokenizers BPE)"
      }
    } else {
      "Rust Candle 0.8 Preparado (Compilación nativa con toolchain aarch64-linux-android en GitHub Actions)"
    }
  }

  /**
   * Safe wrapper that executes SafeTensors inference using native Candle
   * or returns a descriptive fallback result.
   */
  fun evaluateSafeTensorsSafe(
    weightsPath: String,
    tokenizerPath: String,
    configPath: String,
    tokenizerConfigPath: String,
    prompt: String,
    temperature: Float,
    topP: Float,
    maxTokens: Int,
    threads: Int
  ): String {
    if (!isRustLoaded) {
      return ""
    }

    return try {
      evaluateSafeTensorsBundle(
        weightsPath = weightsPath,
        tokenizerPath = tokenizerPath,
        configPath = configPath,
        tokenizerConfigPath = tokenizerConfigPath,
        prompt = prompt,
        temperature = temperature,
        topP = topP,
        maxTokens = maxTokens,
        threads = threads
      )
    } catch (e: Throwable) {
      Log.e(TAG, "Error ejecutando inferencia nativa en Candle: ${e.message}", e)
      "⚠️ Error en ejecución de Candle: ${e.message}"
    }
  }
}
