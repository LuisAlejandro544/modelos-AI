package com.example.engine.formatter

import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.LocalAiModel

object ChatTemplateFormatter {

  fun formatConversation(
    systemPrompt: String,
    history: List<ChatMessage>,
    currentPrompt: String,
    model: LocalAiModel
  ): String {
    val modelName = model.name.lowercase()
    val config = model.configPathOrUri?.lowercase() ?: ""
    val tokConfig = model.tokenizerConfigPathOrUri?.lowercase() ?: ""

    return when {
      modelName.contains("llama-3") || modelName.contains("llama 3") || tokConfig.contains("llama-3") -> {
        formatLlama3Template(systemPrompt, history, currentPrompt)
      }
      modelName.contains("gemma") || config.contains("gemma") -> {
        formatGemmaTemplate(systemPrompt, history, currentPrompt)
      }
      modelName.contains("qwen") || tokConfig.contains("chatml") || modelName.contains("chatml") -> {
        formatChatMLTemplate(systemPrompt, history, currentPrompt)
      }
      modelName.contains("mistral") || modelName.contains("zephyr") -> {
        formatMistralTemplate(systemPrompt, history, currentPrompt)
      }
      else -> {
        formatChatMLTemplate(systemPrompt, history, currentPrompt)
      }
    }
  }

  private fun formatChatMLTemplate(
    systemPrompt: String,
    history: List<ChatMessage>,
    currentPrompt: String
  ): String {
    val sb = StringBuilder()
    if (systemPrompt.isNotBlank()) {
      sb.append("<|im_start|>system\n").append(systemPrompt.trim()).append("<|im_end|>\n")
    }
    history.forEach { msg ->
      val roleStr = if (msg.role == ChatRole.USER) "user" else "assistant"
      if (msg.content.isNotBlank()) {
        sb.append("<|im_start|>").append(roleStr).append("\n").append(msg.content.trim()).append("<|im_end|>\n")
      }
    }
    sb.append("<|im_start|>user\n").append(currentPrompt.trim()).append("<|im_end|>\n")
    sb.append("<|im_start|>assistant\n")
    return sb.toString()
  }

  private fun formatLlama3Template(
    systemPrompt: String,
    history: List<ChatMessage>,
    currentPrompt: String
  ): String {
    val sb = StringBuilder("<|begin_of_text|>")
    if (systemPrompt.isNotBlank()) {
      sb.append("<|start_header_id|>system<|end_header_id|>\n\n").append(systemPrompt.trim()).append("<|eot_id|>")
    }
    history.forEach { msg ->
      val roleStr = if (msg.role == ChatRole.USER) "user" else "assistant"
      if (msg.content.isNotBlank()) {
        sb.append("<|start_header_id|>").append(roleStr).append("<|end_header_id|>\n\n").append(msg.content.trim()).append("<|eot_id|>")
      }
    }
    sb.append("<|start_header_id|>user<|end_header_id|>\n\n").append(currentPrompt.trim()).append("<|eot_id|>")
    sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
    return sb.toString()
  }

  private fun formatGemmaTemplate(
    systemPrompt: String,
    history: List<ChatMessage>,
    currentPrompt: String
  ): String {
    val sb = StringBuilder()
    val effectiveUserPrompt = if (systemPrompt.isNotBlank()) {
      "$systemPrompt\n\n$currentPrompt"
    } else {
      currentPrompt
    }

    history.forEach { msg ->
      val roleTag = if (msg.role == ChatRole.USER) "user" else "model"
      if (msg.content.isNotBlank()) {
        sb.append("<start_of_turn>").append(roleTag).append("\n").append(msg.content.trim()).append("<end_of_turn>\n")
      }
    }

    sb.append("<start_of_turn>user\n").append(effectiveUserPrompt.trim()).append("<end_of_turn>\n")
    sb.append("<start_of_turn>model\n")
    return sb.toString()
  }

  private fun formatMistralTemplate(
    systemPrompt: String,
    history: List<ChatMessage>,
    currentPrompt: String
  ): String {
    val sb = StringBuilder("<s>")
    val combinedFirstUser = if (systemPrompt.isNotBlank()) {
      "[INST] $systemPrompt\n\n$currentPrompt [/INST]"
    } else {
      "[INST] $currentPrompt [/INST]"
    }
    sb.append(combinedFirstUser)
    return sb.toString()
  }
}
