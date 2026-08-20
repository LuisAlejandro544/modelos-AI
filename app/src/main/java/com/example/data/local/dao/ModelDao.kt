package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {

  @Query("SELECT * FROM local_models ORDER BY createdAt DESC")
  fun getAllModels(): Flow<List<ModelEntity>>

  @Query("SELECT * FROM local_models WHERE id = :modelId LIMIT 1")
  suspend fun getModelById(modelId: String): ModelEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertModel(model: ModelEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertModels(models: List<ModelEntity>)

  @Update
  suspend fun updateModel(model: ModelEntity)

  @Query("DELETE FROM local_models WHERE id = :modelId")
  suspend fun deleteModelById(modelId: String)

  @Query("DELETE FROM local_models")
  suspend fun clearAllModels()
}
