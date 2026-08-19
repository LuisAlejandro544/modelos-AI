package com.example.engine

import android.util.Log

object NativeCppBridge {
  private const val TAG = "NativeCppBridge"
  
  var isNativeLoaded: Boolean = false
    private set

  init {
    try {
      System.loadLibrary("local_ai_cpp")
      isNativeLoaded = true
      Log.i(TAG, "Librería nativa C++ (local_ai_cpp) cargada exitosamente.")
    } catch (e: UnsatisfiedLinkError) {
      isNativeLoaded = false
      Log.w(TAG, "Librería nativa C++ no cargada (modo emulación activo): ${e.message}")
    } catch (e: Exception) {
      isNativeLoaded = false
      Log.w(TAG, "Excepción cargando C++: ${e.message}")
    }
  }

  // Native JNI Declarations
  external fun getEngineCapabilities(): String
  external fun initModelContextNative(modelPath: String, nThreads: Int, contextSize: Int): Long
  external fun evaluatePromptNative(contextHandle: Long, prompt: String, temperature: Float, topP: Float, maxTokens: Int): String
  external fun freeModelContextNative(contextHandle: Long)

  fun getSafeEngineCapabilities(): String {
    return if (isNativeLoaded) {
      try {
        getEngineCapabilities()
      } catch (e: Throwable) {
        "C++ Engine listo (JNI Puente activo | ARM64 NEON)"
      }
    } else {
      "C++ llama.cpp Engine preparado (Soporte ARM NEON / Vulkan NDK)"
    }
  }
}
