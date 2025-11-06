package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.LiveResponse
import com.classroom.quizmaster.data.model.LiveSession

interface LiveSessionAgent {
    suspend fun startSession(classId: String, classworkId: String, hostId: String): LiveSession
    suspend fun joinSession(joinCode: String, userId: String): Result<Unit>
    suspend fun submitResponse(response: LiveResponse): Result<Unit>
}
