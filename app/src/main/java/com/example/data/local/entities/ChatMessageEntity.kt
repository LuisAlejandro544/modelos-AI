package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ChatMessage
import com.example.model.ChatRole

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
  @PrimaryKey
  val id: String,
  val sessionId: String,
  val role: String,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val tokensPerSecond: Double? = null,
  val tokenCount: Int? = null,
  val generationTimeMs: Long? = null,
  val ramUsageMb: Int? = null,
  val hardwareUsed: String? = null
) {
  fun toDomainModel(): ChatMessage {
    val domainRole = when (role.uppercase()) {
      "USER" -> ChatRole.USER
      "ASSISTANT" -> ChatRole.ASSISTANT
      else -> ChatRole.SYSTEM
    }

    return ChatMessage(
      id = id,
      role = domainRole,
      content = content,
      timestamp = timestamp,
      metrics = null,
      isStreaming = false,
      liveTokensPerSec = tokensPerSecond,
      liveHardwareInfo = hardwareUsed
    )
  }

  companion object {
    fun fromDomainModel(message: ChatMessage, sessionId: String): ChatMessageEntity {
      return ChatMessageEntity(
        id = message.id,
        sessionId = sessionId,
        role = message.role.name,
        content = message.content,
        timestamp = message.timestamp,
        tokensPerSecond = message.metrics?.tokensPerSecond ?: message.liveTokensPerSec,
        tokenCount = message.metrics?.tokensGenerated,
        generationTimeMs = message.metrics?.generationTimeMs,
        ramUsageMb = message.metrics?.ramUsageMb,
        hardwareUsed = message.metrics?.hardwareUsed ?: message.liveHardwareInfo
      )
    }
  }
}
