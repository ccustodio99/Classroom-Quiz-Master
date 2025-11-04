package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.Attempt
import com.classroom.quizmaster.data.model.Submission
import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.data.repo.AttemptRepository

class AssessmentAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val attemptRepository: AttemptRepository
) : AssessmentAgent {
    override suspend fun start(classworkId: String, userId: String): Attempt {
        TODO("Not yet implemented")
    }

    override suspend fun submit(attempt: Attempt): Result<Submission> {
        TODO("Not yet implemented")
    }
}
