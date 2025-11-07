package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.classroom.quizmaster.data.local.entity.AttemptLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {

    @Query("SELECT * FROM attempts WHERE uid = :uid AND questionId = :questionId LIMIT 1")
    suspend fun getAttempt(uid: String, questionId: String): AttemptLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttempt(attempt: AttemptLocalEntity)

    @Query("SELECT * FROM attempts WHERE uid = :uid")
    fun observeAttempts(uid: String): Flow<List<AttemptLocalEntity>>

    @Query("DELETE FROM attempts")
    suspend fun clear()
}
