package com.acme.lms.agents

import com.acme.lms.data.model.Attempt

interface ScoringAnalyticsAgent {
    suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float>
}
