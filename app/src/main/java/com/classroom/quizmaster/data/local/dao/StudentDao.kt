package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.classroom.quizmaster.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun get(id: String): StudentEntity?

    @Query("SELECT * FROM students")
    fun observeAll(): Flow<List<StudentEntity>>

    @Upsert
    suspend fun upsert(student: StudentEntity)

    @Upsert
    suspend fun upsertAll(students: List<StudentEntity>)
}
