package com.classroom.quizmaster.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE module_id = :moduleId ORDER BY created_at DESC")
    fun observeByModule(moduleId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE id = :id")
    suspend fun get(id: String): AttemptEntity?

    @Query("SELECT * FROM attempts WHERE assessment_id = :assessmentId AND student_id = :studentId")
    suspend fun getByAssessmentAndStudent(assessmentId: String, studentId: String): AttemptEntity?
}
