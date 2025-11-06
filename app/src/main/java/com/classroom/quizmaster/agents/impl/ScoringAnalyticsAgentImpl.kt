package com.classroom.quizmaster.agents.impl

import com.classroom.quizmaster.agents.ScoringAnalyticsAgent
import com.classroom.quizmaster.data.model.Attempt
import com.classroom.quizmaster.data.repo.AnalyticsRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoringAnalyticsAgentImpl @Inject constructor(
    private val analyticsRepo: AnalyticsRepo
) : ScoringAnalyticsAgent {

    override suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float> =
        analyticsRepo.learningGain(preTestAttempt, postTestAttempt)
}
