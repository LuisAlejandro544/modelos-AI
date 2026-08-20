package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalAiDatabase
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.model.ChatMessage
import com.example.model.ChatRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI Local", appName)
  }

  @Test
  fun `test chat session and messages persistence in Room database`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LocalAiDatabase.getDatabase(context)
    val chatDao = db.chatDao()

    val sessionId = UUID.randomUUID().toString()
    val session = ChatSessionEntity(
      id = sessionId,
      title = "Prueba de Chat Local",
      modelId = "model-123",
      modelName = "Llama 3.2 1B GGUF",
      messageCount = 0,
      lastSnippet = "Iniciada",
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )

    chatDao.insertSession(session)
    val retrieved = chatDao.getSessionById(sessionId)
    assertNotNull(retrieved)
    assertEquals("Prueba de Chat Local", retrieved?.title)

    val userMsg = ChatMessage(
      role = ChatRole.USER,
      content = "¿Cómo funciona la inferencia local en CPU/GPU móvil?"
    )
    chatDao.insertMessage(ChatMessageEntity.fromDomainModel(userMsg, sessionId))

    val assistantMsg = ChatMessage(
      role = ChatRole.ASSISTANT,
      content = "La inferencia se realiza mediante llama.cpp o Candle en memoria local sin enviar datos a la nube."
    )
    chatDao.insertMessage(ChatMessageEntity.fromDomainModel(assistantMsg, sessionId))

    val messages = chatDao.getMessagesForSessionList(sessionId)
    assertEquals(2, messages.size)
    assertEquals(ChatRole.USER.name, messages[0].role)
    assertEquals(ChatRole.ASSISTANT.name, messages[1].role)

    // Verify session deletion cleans up
    chatDao.clearMessagesForSession(sessionId)
    chatDao.deleteSessionById(sessionId)
    val afterDelete = chatDao.getSessionById(sessionId)
    assertEquals(null, afterDelete)
    val messagesAfterDelete = chatDao.getMessagesForSessionList(sessionId)
    assertTrue(messagesAfterDelete.isEmpty())
  }
}
