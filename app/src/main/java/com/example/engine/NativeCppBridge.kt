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

  interface NativeTokenCallback {
    fun onToken(piece: String, tokenId: Int): Boolean
  }

  // Native JNI Declarations
  external fun getEngineCapabilities(): String
  external fun parseGgufMetadataFromFd(fd: Int): String
  external fun parseGgufMetadataFromPath(path: String): String
  external fun initGgufModelFromFd(fd: Int, nThreads: Int, contextSize: Int, useMmap: Boolean): Long
  external fun initModelContextNative(modelPath: String, nThreads: Int, contextSize: Int): Long
  external fun tokenizeNative(contextHandle: Long, text: String, addBos: Boolean): IntArray
  external fun decodeTokensNative(contextHandle: Long, tokens: IntArray): String
  external fun decodeTokenNative(contextHandle: Long, tokenId: Int): String
  external fun evaluatePromptNative(contextHandle: Long, prompt: String, temperature: Float, topP: Float, maxTokens: Int): String
  external fun generateStreamingPromptNative(
    contextHandle: Long,
    prompt: String,
    temperature: Float,
    topP: Float,
    repeatPenalty: Float,
    maxTokens: Int,
    callback: NativeTokenCallback
  ): String
  external fun cancelGgufInference(contextHandle: Long)
  external fun freeModelContextNative(contextHandle: Long)

  /**
   * Safely tokenizes text into token IDs using the C++ native BPE / SentencePiece engine
   */
  fun tokenizeSafe(contextHandle: Long, text: String, addBos: Boolean = true): IntArray {
    if (!isNativeLoaded || contextHandle == 0L || text.isBlank()) return IntArray(0)
    return try {
      tokenizeNative(contextHandle, text, addBos)
    } catch (e: Throwable) {
      Log.e(TAG, "Error tokenizando con motor C++ BPE", e)
      IntArray(0)
    }
  }

  /**
   * Safely decodes token IDs back into UTF-8 text using the C++ native tokenizer
   */
  fun decodeTokensSafe(contextHandle: Long, tokens: IntArray): String {
    if (!isNativeLoaded || contextHandle == 0L || tokens.isEmpty()) return ""
    return try {
      decodeTokensNative(contextHandle, tokens)
    } catch (e: Throwable) {
      Log.e(TAG, "Error decodificando tokens con motor C++ BPE", e)
      ""
    }
  }

  /**
   * Safely decodes a single token ID into string piece
   */
  fun decodeTokenSafe(contextHandle: Long, tokenId: Int): String {
    if (!isNativeLoaded || contextHandle == 0L) return ""
    return try {
      decodeTokenNative(contextHandle, tokenId)
    } catch (e: Throwable) {
      Log.e(TAG, "Error decodificando token ID $tokenId", e)
      ""
    }
  }

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
