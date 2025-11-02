package com.classroom.quizmaster.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClassroomEntity)

    @Query("SELECT * FROM classrooms ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms WHERE status = :status ORDER BY name COLLATE NOCASE")
    fun observeByStatus(status: String): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ClassroomEntity?
}
