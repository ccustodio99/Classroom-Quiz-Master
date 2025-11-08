package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.classroom.quizmaster.data.local.entity.TeacherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(teacher: TeacherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(teachers: List<TeacherEntity>)

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    suspend fun get(id: String): TeacherEntity?

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<TeacherEntity?>

    @Query("SELECT * FROM teachers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TeacherEntity>>

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM teachers")
    suspend fun clear()
}
