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

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC LIMIT 1")
    fun observeCurrentSession(): Flow<SessionLocalEntity?>

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC LIMIT 1")
    suspend fun currentSession(): SessionLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionLocalEntity)

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Transaction
    suspend fun replaceSession(session: SessionLocalEntity, participants: List<ParticipantLocalEntity>) {
        clearSessions()
        clearParticipants()
        upsertSession(session)
        if (participants.isNotEmpty()) {
            upsertParticipants(participants)
        }
    }

    @Query(
        "SELECT * FROM participants WHERE sessionId = :sessionId ORDER BY totalPoints DESC, totalTimeMs ASC"
    )
    fun observeParticipants(sessionId: String): Flow<List<ParticipantLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParticipant(participant: ParticipantLocalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParticipants(participants: List<ParticipantLocalEntity>)

    @Query("DELETE FROM participants")
    suspend fun clearParticipants()

    @Query("DELETE FROM participants WHERE sessionId = :sessionId AND uid = :uid")
    suspend fun deleteParticipant(sessionId: String, uid: String)
}
