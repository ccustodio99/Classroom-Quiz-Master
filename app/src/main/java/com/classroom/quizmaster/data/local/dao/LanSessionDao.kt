package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.classroom.quizmaster.data.local.entity.LanSessionMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanSessionDao {

    @Query("SELECT * FROM lan_session_meta LIMIT 1")
    fun observeLatest(): Flow<LanSessionMetaEntity?>

    @Query("SELECT * FROM lan_session_meta WHERE sessionId = :sessionId LIMIT 1")
    suspend fun get(sessionId: String): LanSessionMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: LanSessionMetaEntity)

    @Query("DELETE FROM lan_session_meta WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)

    @Query("DELETE FROM lan_session_meta")
    suspend fun clear()
}
