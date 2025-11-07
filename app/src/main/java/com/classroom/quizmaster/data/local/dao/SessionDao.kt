package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.classroom.quizmaster.data.local.entity.ParticipantLocalEntity
import com.classroom.quizmaster.data.local.entity.SessionLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions LIMIT 1")
    fun observeCurrentSession(): Flow<SessionLocalEntity?>

    @Query("SELECT * FROM sessions LIMIT 1")
    suspend fun currentSession(): SessionLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionLocalEntity)

    @Query("DELETE FROM sessions")
    suspend fun clearSession()

    @Transaction
    suspend fun replaceSession(session: SessionLocalEntity, participants: List<ParticipantLocalEntity>) {
        clearParticipants()
        upsertSession(session)
        upsertParticipants(participants)
    }

    @Query("SELECT * FROM participants ORDER BY totalPoints DESC, totalTimeMs ASC")
    fun observeParticipants(): Flow<List<ParticipantLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParticipants(participants: List<ParticipantLocalEntity>)

    @Query("DELETE FROM participants")
    suspend fun clearParticipants()

    @Query("DELETE FROM participants WHERE uid = :uid")
    suspend fun deleteParticipant(uid: String)
}
