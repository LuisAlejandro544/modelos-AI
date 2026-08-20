package com.example

import com.example.data.repository.ModelRepository
import com.example.engine.formatter.ChatTemplateFormatter
import com.example.engine.metrics.InferenceMetricsTracker
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.LocalAiModel
import com.example.model.ModelFormatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testChatTemplateFormatter_ChatML() {
    val model = LocalAiModel(
      id = "test-qwen",
      name = "Qwen 2.5 1.5B ChatML",
      developer = "Qwen",
      parameterSize = "1.5B",
      quantization = "Q4_K_M",
      ramRequired = "1.2 GB",
      speedEstimate = "35 tok/s",
      recommendedFor = "General",
      formatType = ModelFormatType.GGUF
    )

    val history = listOf(
      ChatMessage(role = ChatRole.USER, content = "Hola"),
      ChatMessage(role = ChatRole.ASSISTANT, content = "¡Hola! ¿Cómo estás?")
    )

    val formatted = ChatTemplateFormatter.formatConversation(
      systemPrompt = "Eres un asistente.",
      history = history,
      currentPrompt = "¿Qué es mmap?",
      model = model
    )

    assertTrue(formatted.contains("<|im_start|>system"))
    assertTrue(formatted.contains("<|im_start|>user\n¿Qué es mmap?<|im_end|>"))
    assertTrue(formatted.endsWith("<|im_start|>assistant\n"))
  }

  @Test
  fun testInferenceMetricsTracker_Estimation() {
    val estimated = InferenceMetricsTracker.estimateTokensFromText("Hola mundo esto es una prueba de tokens")
    assertTrue(estimated > 0)

    val conversationTokens = InferenceMetricsTracker.estimateConversationTokens(
      systemPrompt = "System prompt breve",
      messages = listOf(
        ChatMessage(role = ChatRole.USER, content = "Pregunta"),
        ChatMessage(role = ChatRole.ASSISTANT, content = "Respuesta del modelo local")
      )
    )
    assertTrue(conversationTokens >= 3)
  }

  @Test
  fun testModelRepository_AddAndDelete() {
    val repo = ModelRepository()
    val initialSize = repo.customModels.value.size
    assertEquals(0, initialSize)

    val added = repo.addGgufModel("/data/local/model.gguf", "Mi Modelo GGUF")
    assertEquals("Mi Modelo GGUF", added.name)
    assertEquals(1, repo.customModels.value.size)

    repo.deleteModel(added.id)
    assertEquals(0, repo.customModels.value.size)
  }
}
