package com.classroom.quizmaster.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AssignmentEntity)

    @Query("SELECT * FROM assignments WHERE module_id = :moduleId")
    fun observeForModule(moduleId: String): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun get(id: String): AssignmentEntity?

    @Query("SELECT * FROM assignments WHERE id = :id")
    fun observeById(id: String): Flow<AssignmentEntity?>
}
