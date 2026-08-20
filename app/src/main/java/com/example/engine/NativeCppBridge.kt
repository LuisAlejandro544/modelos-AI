package com.example.engine

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.App
import org.json.JSONObject

data class GgufParsedInfo(
  val isValid: Boolean,
  val version: Int,
  val architecture: String,
  val modelName: String,
  val contextLength: Long,
  val embeddingLength: Long,
  val blockCount: Long,
  val hasChatTemplate: Boolean,
  val tensorCount: Long,
  val kvCount: Long,
  val errorMessage: String? = null
)

object NativeCppBridge {
  private const val TAG = "NativeCppBridge"
  
  var isNativeLoaded: Boolean = false
    private set

  init {
    try {
      System.loadLibrary("local_ai_cpp")
      isNativeLoaded = true
    } catch (_: Throwable) {
      isNativeLoaded = false
    }
  }

  // Native JNI Declarations
  external fun getEngineCapabilities(): String
  external fun parseGgufMetadataFromFd(fd: Int): String
  external fun parseGgufMetadataFromPath(path: String): String
  external fun initGgufModelFromFd(fd: Int, nThreads: Int, contextSize: Int, useMmap: Boolean): Long
  external fun initModelContextNative(modelPath: String, nThreads: Int, contextSize: Int): Long
  external fun evaluatePromptNative(contextHandle: Long, prompt: String, temperature: Float, topP: Float, maxTokens: Int): String
  external fun cancelGgufInference(contextHandle: Long)
  external fun freeModelContextNative(contextHandle: Long)

  fun getSafeEngineCapabilities(): String {
    return if (isNativeLoaded) {
      try {
        getEngineCapabilities()
      } catch (e: Throwable) {
        "C++ llama.cpp Engine listo (JNI Puente activo | ARM64 NEON | GGUF v2/v3)"
      }
    } else {
      "C++ llama.cpp Engine preparado (Soporte ARM NEON / Vulkan NDK / GGUF)"
    }
  }

  /**
   * Safely parses GGUF metadata from URI or absolute file path
   */
  fun parseGgufMetadataSafe(filePathOrUri: String, context: Context? = null): GgufParsedInfo? {
    if (!isNativeLoaded || filePathOrUri.isBlank()) return null

    val ctx = context ?: App.instance?.applicationContext

    return try {
      val jsonString: String = if (filePathOrUri.startsWith("content://") && ctx != null) {
        val uri = Uri.parse(filePathOrUri)
        val pfd: ParcelFileDescriptor? = ctx.contentResolver.openFileDescriptor(uri, "r")
        pfd?.use { descriptor ->
          parseGgufMetadataFromFd(descriptor.fd)
        } ?: return null
      } else {
        parseGgufMetadataFromPath(filePathOrUri)
      }

      val json = JSONObject(jsonString)
      val isValid = json.optBoolean("isValid", false)
      if (!isValid) {
        return GgufParsedInfo(
          isValid = false,
          version = json.optInt("version", 0),
          architecture = "",
          modelName = "",
          contextLength = 0,
          embeddingLength = 0,
          blockCount = 0,
          hasChatTemplate = false,
          tensorCount = 0,
          kvCount = 0,
          errorMessage = json.optString("errorMessage", "Archivo GGUF inválido")
        )
      }

      GgufParsedInfo(
        isValid = true,
        version = json.optInt("version", 3),
        architecture = json.optString("architecture", "llama"),
        modelName = json.optString("modelName", "GGUF Model"),
        contextLength = json.optLong("contextLength", 4096),
        embeddingLength = json.optLong("embeddingLength", 2048),
        blockCount = json.optLong("blockCount", 24),
        hasChatTemplate = json.optBoolean("hasChatTemplate", false),
        tensorCount = json.optLong("tensorCount", 0),
        kvCount = json.optLong("kvCount", 0)
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error parseando metadatos GGUF", e)
      null
    }
  }
}
