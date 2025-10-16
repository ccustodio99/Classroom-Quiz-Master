package com.classroom.quizmaster.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules")
    fun observeModules(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules")
    suspend fun getAll(): List<ModuleEntity>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getModule(id: String): ModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ModuleEntity)

    @Update
    suspend fun update(entity: ModuleEntity)
}
