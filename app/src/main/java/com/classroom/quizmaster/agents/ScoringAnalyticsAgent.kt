package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.Attempt

interface ScoringAnalyticsAgent {
    suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float>
}
