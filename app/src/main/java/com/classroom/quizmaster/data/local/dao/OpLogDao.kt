package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.classroom.quizmaster.data.local.entity.OpLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entry: OpLogEntity)

    @Query("SELECT * FROM oplog WHERE synced = 0 ORDER BY ts ASC LIMIT :limit")
    suspend fun pending(limit: Int = 50): List<OpLogEntity>

    @Query("UPDATE oplog SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM oplog WHERE synced = 1")
    suspend fun deleteSynced()

    @Query("SELECT COUNT(*) FROM oplog WHERE synced = 0")
    fun observePendingCount(): Flow<Int>
}
