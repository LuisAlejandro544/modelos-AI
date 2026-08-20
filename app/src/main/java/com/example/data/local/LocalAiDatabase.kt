package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ModelDao
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.ModelEntity

@Database(
  entities = [
    ModelEntity::class,
    ChatSessionEntity::class,
    ChatMessageEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class LocalAiDatabase : RoomDatabase() {

  abstract fun modelDao(): ModelDao
  abstract fun chatDao(): ChatDao

  companion object {
    @Volatile
    private var INSTANCE: LocalAiDatabase? = null

    fun getDatabase(context: Context): LocalAiDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LocalAiDatabase::class.java,
          "local_ai_database.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
