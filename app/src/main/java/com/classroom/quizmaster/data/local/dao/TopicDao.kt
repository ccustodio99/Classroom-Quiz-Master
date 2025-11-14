package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.classroom.quizmaster.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(topics: List<TopicEntity>)

    @Query("SELECT * FROM topics WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun observeForTeacher(teacherId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE classroomId = :classroomId AND isArchived = 0 ORDER BY createdAt DESC")
    fun observeActiveForClassroom(classroomId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun get(id: String): TopicEntity?
}
