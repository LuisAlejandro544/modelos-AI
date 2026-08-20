package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
  @PrimaryKey
  val id: String,
  val title: String,
  val modelId: String,
  val modelName: String = "",
  val systemPrompt: String? = null,
  val messageCount: Int = 0,
  val lastSnippet: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
