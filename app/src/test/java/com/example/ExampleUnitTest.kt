package com.example

import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ModelEntity
import com.example.data.repository.ModelRepository
import com.example.engine.formatter.ChatTemplateFormatter
import com.example.engine.metrics.InferenceMetricsTracker
import com.example.engine.tokenizer.TextDetokenizer
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
  fun testTextDetokenizer() {
    val rawPiece = "ĠHolaĠmundoĊ"
    val cleaned = TextDetokenizer.cleanPiece(rawPiece)
    assertEquals(" Hola mundo\n", cleaned)

    val hexPiece = "prueba<0x20>espacio"
    val cleanedHex = TextDetokenizer.cleanFullText(hexPiece)
    assertEquals("prueba espacio", cleanedHex)

    val multiByteUtf8 = "cami<0xC3><0xB3>n"
    val cleanedMultiByte = TextDetokenizer.cleanFullText(multiByteUtf8)
    assertEquals("camión", cleanedMultiByte)

    val corruptedWithArtifact = "texto\uFFFDlimpio"
    val cleanedArtifact = TextDetokenizer.cleanFullText(corruptedWithArtifact)
    assertEquals("textolimpio", cleanedArtifact)
  }

  @Test
  fun testRoomEntityMapping() {
    val domainModel = LocalAiModel(
      id = "test-id",
      name = "Test Model",
      developer = "Dev",
      parameterSize = "1B",
      quantization = "Q4",
      ramRequired = "1GB",
      speedEstimate = "30 tok/s",
      recommendedFor = "General",
      formatType = ModelFormatType.GGUF
    )

    val entity = ModelEntity.fromDomainModel(domainModel)
    val convertedBack = entity.toDomainModel()

    assertEquals(domainModel.id, convertedBack.id)
    assertEquals(domainModel.name, convertedBack.name)
    assertEquals(domainModel.formatType, convertedBack.formatType)

    val chatMsg = ChatMessage(
      role = ChatRole.USER,
      content = "Hola desde test"
    )
    val msgEntity = ChatMessageEntity.fromDomainModel(chatMsg, "session-1")
    val msgBack = msgEntity.toDomainModel()

    assertEquals(chatMsg.id, msgBack.id)
    assertEquals(chatMsg.content, msgBack.content)
    assertEquals(ChatRole.USER, msgBack.role)
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

  @Test
  fun testInferenceParameters_SanitizationAndModelBounds() {
    val smallModel = LocalAiModel(
      id = "smol-model",
      name = "SmolLM2 135M",
      developer = "HuggingFace",
      parameterSize = "135M",
      quantization = "Q4_K_M",
      ramRequired = "200 MB",
      speedEstimate = "50 tok/s",
      recommendedFor = "Pruebas",
      contextLength = 2048,
      formatType = ModelFormatType.GGUF
    )

    // Intentar configurar valores extremos o fuera del límite del modelo
    val extremeParams = com.example.model.InferenceParameters(
      contextWindow = 32768, // Supera el límite de 2048
      maxTokens = 4096,     // Supera el contextWindow resultante
      temperature = 5.0f,   // Supera el rango seguro de 2.0
      topP = 1.5f,          // Supera 1.0
      topK = 500,           // Supera 100
      repeatPenalty = 3.0f, // Supera 2.0
      cpuThreads = 64       // Supera núcleos disponibles
    )

    val sanitized = extremeParams.sanitize(model = smallModel, maxAvailableCores = 8)

    assertEquals(2048, sanitized.contextWindow) // Acotado al límite nativo del modelo
    assertEquals(2048, sanitized.maxTokens)     // Acotado a contextWindow
    assertEquals(2.0f, sanitized.temperature, 0.01f) // Acotado al rango seguro
    assertEquals(1.0f, sanitized.topP, 0.01f)
    assertEquals(100, sanitized.topK)
    assertEquals(2.0f, sanitized.repeatPenalty, 0.01f)
    assertEquals(8, sanitized.cpuThreads)

    // Verificar con valores por debajo del mínimo permitido
    val lowerParams = com.example.model.InferenceParameters(
      contextWindow = 50,
      maxTokens = 2,
      temperature = -1.0f,
      topP = -0.5f,
      topK = 0,
      repeatPenalty = 0.2f,
      cpuThreads = 0
    )

    val sanitizedLower = lowerParams.sanitize(model = smallModel, maxAvailableCores = 4)
    assertEquals(256, sanitizedLower.contextWindow)
    assertEquals(16, sanitizedLower.maxTokens)
    assertEquals(0.0f, sanitizedLower.temperature, 0.01f)
    assertEquals(0.01f, sanitizedLower.topP, 0.01f)
    assertEquals(1, sanitizedLower.topK)
    assertEquals(1.0f, sanitizedLower.repeatPenalty, 0.01f)
    assertEquals(1, sanitizedLower.cpuThreads)
  }

  @Test
  fun testTFLiteModel_EntityAndFormatMapping() {
    val tfliteModel = LocalAiModel(
      id = "tflite-test",
      name = "Gemma 2B TFLite",
      developer = "Google",
      parameterSize = "2B",
      quantization = "INT8",
      ramRequired = "1.2 GB",
      speedEstimate = "35 tok/s",
      recommendedFor = "Inferencia móvil NPU",
      contextLength = 4096,
      formatType = ModelFormatType.TFLITE,
      filePathOrUri = "/sdcard/Download/gemma-2b.tflite",
      tokenizerPathOrUri = "/sdcard/Download/tokenizer.json"
    )

    val entity = com.example.data.local.entities.ModelEntity.fromDomainModel(tfliteModel)
    assertEquals("TFLITE", entity.formatType)

    val restored = entity.toDomainModel()
    assertEquals(ModelFormatType.TFLITE, restored.formatType)
    assertEquals(".tflite", restored.formatType.extension)
    assertEquals(true, restored.formatType.requiresSeparateTokenizer)
  }
}
