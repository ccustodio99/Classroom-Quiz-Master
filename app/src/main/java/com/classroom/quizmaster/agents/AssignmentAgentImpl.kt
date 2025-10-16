package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.AssignmentRepository
import com.classroom.quizmaster.domain.model.Assignment
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

class AssignmentAgentImpl(
    private val repository: AssignmentRepository
) : AssignmentAgent {
    override suspend fun assign(moduleId: String, dueEpochMs: Long): Assignment {
        val assignment = Assignment(
            id = UUID.randomUUID().toString(),
            moduleId = moduleId,
            dueEpochMs = dueEpochMs
        )
        repository.upsert(assignment)
        return assignment
    }

    override fun status(assignmentId: String): Flow<Assignment> {
        return repository.observeAssignment(assignmentId).filterNotNull()
    }
}
