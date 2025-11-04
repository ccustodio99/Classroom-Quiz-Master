package com.acme.lms.agents.impl

import com.acme.lms.agents.ScoringAnalyticsAgent
import com.acme.lms.data.model.Attempt
import com.acme.lms.data.repo.AnalyticsRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoringAnalyticsAgentImpl @Inject constructor(
    private val analyticsRepo: AnalyticsRepo
) : ScoringAnalyticsAgent {

    override suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float> =
        analyticsRepo.learningGain(preTestAttempt, postTestAttempt)
}
