package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.Attempt

interface ScoringAnalyticsAgent {
  suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float>
}
