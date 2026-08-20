package com.example.data.repository

import com.example.App
import com.example.data.local.LocalAiDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.model.ChatMessage
import com.example.model.ChatRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository(
  private val chatDao: ChatDao? = try {
    App.instance?.let { LocalAiDatabase.getDatabase(it).chatDao() }
  } catch (_: Throwable) {
    null
  },
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

  private val _sessions = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
  val sessions: StateFlow<List<ChatSessionEntity>> = _sessions.asStateFlow()

  init {
    if (chatDao != null) {
      coroutineScope.launch {
        try {
          chatDao.getAllSessions().collect { list ->
            _sessions.value = list
          }
        } catch (_: Throwable) {}
      }
    }
  }

  suspend fun createNewSession(
    modelId: String,
    modelName: String,
    systemPrompt: String? = null,
    initialTitle: String = "Nueva conversación"
  ): ChatSessionEntity {
    val newSession = ChatSessionEntity(
      id = UUID.randomUUID().toString(),
      title = initialTitle,
      modelId = modelId,
      modelName = modelName,
      systemPrompt = systemPrompt,
      messageCount = 0,
      lastSnippet = "Conversación iniciada",
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )

    _sessions.value = listOf(newSession) + _sessions.value.filterNot { it.id == newSession.id }

    if (chatDao != null) {
      try {
        chatDao.insertSession(newSession)
      } catch (_: Throwable) {}
    }

    return newSession
  }

  suspend fun loadMessagesForSession(sessionId: String, modelName: String = "Modelo Local"): List<ChatMessage> {
    if (chatDao == null) return emptyList()
    return try {
      val entities = chatDao.getMessagesForSessionList(sessionId)
      entities.map { it.toDomainModel(modelName) }
    } catch (_: Throwable) {
      emptyList()
    }
  }

  suspend fun saveMessage(sessionId: String, message: ChatMessage, modelName: String = "") {
    if (sessionId.isBlank()) return

    val entity = ChatMessageEntity.fromDomainModel(message, sessionId)

    // Update in room
    if (chatDao != null) {
      try {
        chatDao.insertMessage(entity)

        val currentSession = chatDao.getSessionById(sessionId)
        if (currentSession != null) {
          val snippet = if (message.content.length > 60) {
            message.content.take(57) + "..."
          } else {
            message.content.ifBlank { "..." }
          }

          val updatedTitle = if (
            (currentSession.title == "Nueva conversación" || currentSession.title.isBlank()) &&
            message.role == ChatRole.USER &&
            message.content.isNotBlank()
          ) {
            val clean = message.content.trim().lines().firstOrNull() ?: message.content
            if (clean.length > 36) clean.take(33) + "..." else clean
          } else {
            currentSession.title
          }

          val updatedSession = currentSession.copy(
            title = updatedTitle,
            modelName = if (modelName.isNotBlank()) modelName else currentSession.modelName,
            messageCount = currentSession.messageCount + 1,
            lastSnippet = snippet,
            updatedAt = System.currentTimeMillis()
          )
          chatDao.updateSession(updatedSession)
        }
      } catch (_: Throwable) {}
    }
  }

  suspend fun updateSessionTitle(sessionId: String, newTitle: String) {
    if (chatDao != null) {
      try {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
          chatDao.updateSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
      } catch (_: Throwable) {}
    }
  }

  suspend fun deleteSession(sessionId: String) {
    _sessions.value = _sessions.value.filterNot { it.id == sessionId }
    if (chatDao != null) {
      try {
        chatDao.clearMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
      } catch (_: Throwable) {}
    }
  }

  suspend fun clearSessionMessages(sessionId: String) {
    if (chatDao != null) {
      try {
        chatDao.clearMessagesForSession(sessionId)
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
          chatDao.updateSession(
            session.copy(
              messageCount = 0,
              lastSnippet = "Conversación vaciada",
              updatedAt = System.currentTimeMillis()
            )
          )
        }
      } catch (_: Throwable) {}
    }
  }
}
