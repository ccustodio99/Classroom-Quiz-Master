package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Submission
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface AssignmentRepository {
    val assignments: Flow<List<Assignment>>
    fun submissions(assignmentId: String): Flow<List<Submission>>
    fun submissionsForUser(userId: String): Flow<List<Submission>>
    suspend fun refreshAssignments()
    suspend fun getAssignment(id: String): Assignment?
    suspend fun createAssignment(assignment: Assignment)
    suspend fun updateAssignment(assignment: Assignment)
    suspend fun archiveAssignment(id: String, archivedAt: Instant = Clock.System.now())
    suspend fun submitHomework(submission: Submission)
}
