package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.classroom.quizmaster.data.local.entity.JoinRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JoinRequestDao {
    @Query("SELECT * FROM join_requests WHERE id = :id")
    suspend fun get(id: String): JoinRequestEntity?

    @Query("SELECT * FROM join_requests WHERE teacherId = :teacherId")
    fun observeForTeacher(teacherId: String): Flow<List<JoinRequestEntity>>

    @Upsert
    suspend fun upsert(joinRequest: JoinRequestEntity)

    @Upsert
    suspend fun upsertAll(joinRequests: List<JoinRequestEntity>)
}
