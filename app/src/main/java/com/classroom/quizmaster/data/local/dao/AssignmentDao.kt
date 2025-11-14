package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.classroom.quizmaster.data.local.entity.AssignmentLocalEntity
import com.classroom.quizmaster.data.local.entity.SubmissionLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {

    @Query("SELECT * FROM assignments ORDER BY openAt DESC")
    fun observeAssignments(): Flow<List<AssignmentLocalEntity>>

    @Query("SELECT * FROM assignments WHERE id = :id LIMIT 1")
    suspend fun getAssignment(id: String): AssignmentLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(assignments: List<AssignmentLocalEntity>)

    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId")
    fun observeSubmissions(assignmentId: String): Flow<List<SubmissionLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubmission(submission: SubmissionLocalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubmissions(submissions: List<SubmissionLocalEntity>)

}
