package com.example.lms.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.lms.core.database.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entity: OutboxEntity): Long

    @Query("SELECT * FROM outbox ORDER BY id ASC")
    fun observeOutbox(): Flow<List<OutboxEntity>>

    @Query("DELETE FROM outbox WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Transaction
    suspend fun dequeue(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            deleteByIds(ids)
        }
    }
}

