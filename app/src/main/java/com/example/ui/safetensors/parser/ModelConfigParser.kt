package com.example.ui.safetensors.parser

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class ConfigMetadata(
  val modelType: String,
  val estimatedParams: String,
  val torchDtype: String,
  val contextLength: Int
)

data class TokenizerConfigMetadata(
  val chatTemplateSummary: String,
  val bosToken: String?,
  val eosToken: String?
)

object ModelConfigParser {

  fun parseConfigJson(context: Context, uri: Uri): ConfigMetadata? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = reader.readText()
        val json = JSONObject(content)

        val modelType = json.optString("model_type", "")
        val hiddenSize = json.optInt("hidden_size", 0)
        val numLayers = json.optInt("num_hidden_layers", 0)
        val intermediateSize = json.optInt("intermediate_size", 0)
        val torchDtype = json.optString("torch_dtype", json.optString("dtype", "f16"))
        val maxPositions = json.optInt("max_position_embeddings", 4096)

        val estimatedParams = when {
          hiddenSize > 0 && numLayers > 0 -> {
            val totalParams = (hiddenSize.toLong() * hiddenSize * numLayers * 4) +
              (hiddenSize.toLong() * intermediateSize * numLayers * 3)
            when {
              totalParams > 2_500_000_000L -> "3B"
              totalParams > 1_200_000_000L -> "1.5B"
              totalParams > 600_000_000L -> "0.8B"
              totalParams > 250_000_000L -> "0.5B"
              else -> "0.3B"
            }
          }
          else -> ""
        }

        ConfigMetadata(
          modelType = modelType,
          estimatedParams = estimatedParams,
          torchDtype = torchDtype,
          contextLength = maxPositions
        )
      }
    } catch (e: Exception) {
      null
    }
  }

  fun parseTokenizerConfigJson(context: Context, uri: Uri): TokenizerConfigMetadata? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = reader.readText()
        val json = JSONObject(content)

        val chatTemplate = json.optString("chat_template", "")
        val summary = when {
          chatTemplate.contains("<|im_start|>") -> "ChatML (<|im_start|>user / assistant)"
          chatTemplate.contains("<|start_header_id|>") -> "Llama 3 (<|start_header_id|>)"
          chatTemplate.contains("<start_of_turn>") -> "Gemma (<start_of_turn>user / model)"
          chatTemplate.contains("[INST]") -> "Mistral / Llama 2 ([INST] ... [/INST])"
          chatTemplate.isNotBlank() -> "Jinja Template Personalizado"
          else -> "Estandar"
        }

        TokenizerConfigMetadata(
          chatTemplateSummary = summary,
          bosToken = if (json.has("bos_token")) json.optString("bos_token") else null,
          eosToken = if (json.has("eos_token")) json.optString("eos_token") else null
        )
      }
    } catch (e: Exception) {
      null
    }
  }
}
