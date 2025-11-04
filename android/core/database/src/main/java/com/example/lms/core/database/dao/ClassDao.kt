package com.example.lms.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lms.core.database.entity.ClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(classes: List<ClassEntity>)

    @Query("SELECT * FROM classes")
    fun observeClasses(): Flow<List<ClassEntity>>
}

