package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.LiveSession
import com.classroom.quizmaster.data.model.LiveResponse

class LiveSessionAgentImpl : LiveSessionAgent {
    override suspend fun startSession(classworkId: String, hostId: String): LiveSession {
        TODO("Not yet implemented")
    }

    override suspend fun joinSession(joinCode: String, userId: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun submitResponse(response: LiveResponse): Result<Unit> {
        TODO("Not yet implemented")
    }
}
