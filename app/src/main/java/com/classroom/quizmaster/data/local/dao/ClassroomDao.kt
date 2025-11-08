package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.classroom.quizmaster.data.local.entity.ClassroomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(classroom: ClassroomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(classrooms: List<ClassroomEntity>)

    @Query("SELECT * FROM classrooms WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun observeForTeacher(teacherId: String): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ClassroomEntity?

    @Query("DELETE FROM classrooms WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM classrooms")
    suspend fun clear()
}
