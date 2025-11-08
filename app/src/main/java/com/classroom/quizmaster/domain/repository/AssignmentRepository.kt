package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Submission
import kotlinx.coroutines.flow.Flow

interface AssignmentRepository {
    val assignments: Flow<List<Assignment>>
    fun submissions(assignmentId: String): Flow<List<Submission>>
    suspend fun refreshAssignments()
    suspend fun createAssignment(assignment: Assignment)
    suspend fun submitHomework(submission: Submission)
}
