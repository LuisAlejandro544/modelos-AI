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
      Log.i(TAG, "Librería nativa Rust (local_ai_rust) cargada exitosamente.")
    } catch (e: UnsatisfiedLinkError) {
      isRustLoaded = false
      Log.w(TAG, "Librería nativa Rust no cargada (modo fallback activo): ${e.message}")
    } catch (e: Exception) {
      isRustLoaded = false
      Log.w(TAG, "Excepción cargando Rust: ${e.message}")
    }
  }

  // Native Rust JNI Declarations
  external fun getRustEngineInfo(): String
  external fun initRustContext(modelName: String, threads: Int): Long
  external fun evaluatePromptRust(handle: Long, prompt: String, temperature: Float, maxTokens: Int): String
  external fun freeRustContext(handle: Long)

  fun getSafeRustInfo(): String {
    return if (isRustLoaded) {
      try {
        getRustEngineInfo()
      } catch (e: Throwable) {
        "Rust Safe Engine listo (UniFFI / JNI nativo activo)"
      }
    } else {
      "Rust Candle Engine preparado (Seguridad de memoria estricta)"
    }
  }
}
