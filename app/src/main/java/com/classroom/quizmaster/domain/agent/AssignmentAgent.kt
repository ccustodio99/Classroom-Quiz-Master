package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.Assignment
import kotlinx.coroutines.flow.Flow

interface AssignmentAgent {
    suspend fun assign(moduleId: String, dueEpochMs: Long): Assignment
    fun status(assignmentId: String): Flow<Assignment>
}
