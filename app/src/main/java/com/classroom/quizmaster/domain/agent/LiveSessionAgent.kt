package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.LiveResponse
import com.classroom.quizmaster.domain.model.LiveSession

interface LiveSessionAgent {
  suspend fun startSession(classworkId: String, hostId: String): LiveSession
  suspend fun joinSession(joinCode: String, userId: String): Result<Unit>
  suspend fun submitResponse(response: LiveResponse): Result<Unit>
}
