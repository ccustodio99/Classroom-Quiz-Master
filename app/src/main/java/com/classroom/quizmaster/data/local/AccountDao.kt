package com.classroom.quizmaster.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AccountEntity)

    @Update
    suspend fun update(entity: AccountEntity)

    @Query("SELECT * FROM accounts WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE status = :status ORDER BY created_at ASC")
    fun observeByStatus(status: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY created_at ASC")
    fun observeAll(): Flow<List<AccountEntity>>
}
