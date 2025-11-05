package com.acme.lms.agents

import com.example.lms.core.model.LiveResponse
import com.example.lms.core.model.LiveSession

interface LiveSessionAgent {
    suspend fun startSession(classId: String, classworkId: String, hostId: String): LiveSession
    suspend fun joinSession(joinCode: String, userId: String): Result<Unit>
    suspend fun submitResponse(response: LiveResponse): Result<Unit>
}
