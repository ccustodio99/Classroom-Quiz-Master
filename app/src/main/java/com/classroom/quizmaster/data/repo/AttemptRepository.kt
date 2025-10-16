package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.model.Attempt
import kotlinx.coroutines.flow.Flow

interface AttemptRepository {
    suspend fun upsert(attempt: Attempt)
    suspend fun find(attemptId: String): Attempt?
    suspend fun findByAssessmentAndStudent(assessmentId: String, studentId: String): Attempt?
    fun observeByModule(moduleId: String): Flow<List<Attempt>>
}
