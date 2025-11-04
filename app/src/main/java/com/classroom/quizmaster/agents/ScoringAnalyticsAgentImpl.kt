package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.Attempt
import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.data.repo.AttemptRepository

class ScoringAnalyticsAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val attemptRepository: AttemptRepository
) : ScoringAnalyticsAgent {
    override suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float> {
        TODO("Not yet implemented")
    }
}
