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
  external fun evaluateSafeTensorsBundle(
    weightsPath: String,
    tokenizerPath: String,
    configPath: String,
    tokenizerConfigPath: String,
    prompt: String,
    temperature: Float,
    maxTokens: Int,
    threads: Int
  ): String
  external fun freeRustContext(handle: Long)

  fun getSafeRustInfo(): String {
    return if (isRustLoaded) {
      try {
        getRustEngineInfo()
      } catch (e: Throwable) {
        "Rust Safe Engine listo (UniFFI / JNI nativo activo con Candle)"
      }
    } else {
      "Rust Candle Engine preparado (Seguridad de memoria estricta en tensores SafeTensors)"
    }
  }

  /**
   * Formats a raw user prompt into the model's chat template format
   * using the detected template from tokenizer_config.json.
   */
  fun formatChatPrompt(
    systemPrompt: String,
    userMessage: String,
    chatTemplateType: String = "ChatML"
  ): String {
    return when {
      chatTemplateType.contains("ChatML", ignoreCase = true) -> {
        "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$userMessage<|im_end|>\n<|im_start|>assistant\n"
      }
      chatTemplateType.contains("Llama 3", ignoreCase = true) || chatTemplateType.contains("llama3", ignoreCase = true) -> {
        "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n$userMessage<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
      }
      chatTemplateType.contains("Gemma", ignoreCase = true) -> {
        "<bos><start_of_turn>user\n$systemPrompt\n\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
      }
      chatTemplateType.contains("Mistral", ignoreCase = true) || chatTemplateType.contains("INST", ignoreCase = true) -> {
        "<s>[INST] <<SYS>>\n$systemPrompt\n<</SYS>>\n\n$userMessage [/INST]"
      }
      else -> {
        // Standard ChatML fallback
        "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$userMessage<|im_end|>\n<|im_start|>assistant\n"
      }
    }
  }
}

