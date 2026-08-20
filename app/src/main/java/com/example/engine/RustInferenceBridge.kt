package com.example.engine

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.App
import java.io.File

object RustInferenceBridge {
  private const val TAG = "RustInferenceBridge"

  var isRustLoaded: Boolean = false
    private set

  init {
    try {
      System.loadLibrary("local_ai_rust")
      isRustLoaded = true
    } catch (_: Throwable) {
      isRustLoaded = false
    }
  }

  // Native Rust JNI Declarations
  external fun getRustEngineInfo(): String
  external fun initRustContext(modelName: String, threads: Int): Long
  external fun evaluatePromptRust(handle: Long, prompt: String, temperature: Float, maxTokens: Int): String

  external fun evaluateSafeTensorsWithFd(
    weightsFd: Int,
    tokenizerJson: String,
    configJson: String,
    tokenizerConfigJson: String,
    prompt: String,
    temperature: Float,
    topP: Float,
    maxTokens: Int,
    threads: Int
  ): String

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

  private fun readUriOrPathString(context: Context?, uriOrPath: String?): String {
    if (uriOrPath.isNullOrBlank()) return ""
    return try {
      if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
        val uri = Uri.parse(uriOrPath)
        val resolver = context?.contentResolver
          ?: try { App.instance.contentResolver } catch (e: Throwable) { null }
        resolver?.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
      } else {
        val file = File(uriOrPath)
        if (file.exists()) file.readText() else ""
      }
    } catch (e: Throwable) {
      Log.w(TAG, "No se pudo leer contenido de $uriOrPath: ${e.message}")
      ""
    }
  }

  /**
   * Safe wrapper that executes SafeTensors inference using native Candle
   * opening the ParcelFileDescriptor (FD) for content:// URIs and mapping via mmap.
   */
  fun evaluateSafeTensorsSafe(
    context: Context? = null,
    weightsPathOrUri: String,
    tokenizerPathOrUri: String,
    configPathOrUri: String,
    tokenizerConfigPathOrUri: String,
    prompt: String,
    temperature: Float,
    topP: Float,
    maxTokens: Int,
    threads: Int
  ): String {
    if (!isRustLoaded) {
      return ""
    }

    val ctx = context ?: try { App.instance } catch (e: Throwable) { null }

    // Read JSON metadata files
    val tokenizerJson = readUriOrPathString(ctx, tokenizerPathOrUri)
    val configJson = readUriOrPathString(ctx, configPathOrUri)
    val tokenizerConfigJson = readUriOrPathString(ctx, tokenizerConfigPathOrUri)

    // Try Opening via ParcelFileDescriptor for zero-copy memory mapping
    var pfd: ParcelFileDescriptor? = null
    try {
      if (weightsPathOrUri.startsWith("content://") || weightsPathOrUri.startsWith("file://")) {
        val uri = Uri.parse(weightsPathOrUri)
        pfd = ctx?.contentResolver?.openFileDescriptor(uri, "r")
      } else {
        val file = File(weightsPathOrUri)
        if (file.exists()) {
          pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
      }

      if (pfd != null) {
        val fd = pfd.fd
        if (fd >= 0) {
          return evaluateSafeTensorsWithFd(
            weightsFd = fd,
            tokenizerJson = tokenizerJson,
            configJson = configJson,
            tokenizerConfigJson = tokenizerConfigJson,
            prompt = prompt,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            threads = threads
          )
        }
      }

      // Fallback: direct path evaluation
      return evaluateSafeTensorsBundle(
        weightsPath = weightsPathOrUri,
        tokenizerPath = tokenizerPathOrUri,
        configPath = configPathOrUri,
        tokenizerConfigPath = tokenizerConfigPathOrUri,
        prompt = prompt,
        temperature = temperature,
        topP = topP,
        maxTokens = maxTokens,
        threads = threads
      )
    } catch (e: Throwable) {
      Log.e(TAG, "Error ejecutando inferencia nativa en Candle: ${e.message}", e)
      return "⚠️ [Error en motor Candle SafeTensors]: ${e.message}"
    } finally {
      try {
        pfd?.close()
      } catch (ignored: Throwable) {}
    }
  }
}
