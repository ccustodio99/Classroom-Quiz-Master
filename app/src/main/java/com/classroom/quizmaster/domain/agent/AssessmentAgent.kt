package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Submission

interface AssessmentAgent {
  suspend fun start(classworkId: String, userId: String): Attempt
  suspend fun submit(attempt: Attempt): Result<Submission>
}
