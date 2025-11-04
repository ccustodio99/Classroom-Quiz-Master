package com.classroom.quizmaster.domain.agent.impl

import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.data.local.SyncEntityType
import com.classroom.quizmaster.domain.agent.LiveSessionAgent
import com.classroom.quizmaster.domain.model.LiveResponse
import com.classroom.quizmaster.domain.model.LiveSession
import java.util.UUID

class LiveSessionAgentImpl(
    private val localData: BlueprintLocalDataSource
) : LiveSessionAgent {

    override suspend fun startSession(classworkId: String, hostId: String): LiveSession {
        val session = LiveSession(
            id = UUID.randomUUID().toString(),
            classworkId = classworkId,
            hostId = hostId,
            joinCode = generateJoinCode(),
            isActive = true,
            currentQuestionId = null
        )
        localData.upsertLiveSession(session)
        localData.enqueueSync(
            entityType = SyncEntityType.LIVE_SESSION,
            entityId = session.id,
            payload = session
        )
        return session
    }

    override suspend fun joinSession(joinCode: String, userId: String): Result<Unit> = runCatching {
        val session = localData.liveSessionByJoinCode(joinCode)
            ?: throw IllegalArgumentException("Invalid session code.")
        if (!session.isActive) throw IllegalStateException("Session already ended.")
        // TODO: track participants via PresenceAgent
    }

    override suspend fun submitResponse(response: LiveResponse): Result<Unit> = runCatching {
        val existing = localData.liveSession(response.liveSessionId)
            ?: throw IllegalArgumentException("Session not found.")
        if (!existing.isActive) throw IllegalStateException("Session already ended.")
        val normalized = if (response.id.isBlank()) {
            response.copy(id = UUID.randomUUID().toString())
        } else {
            response
        }
        localData.recordLiveResponse(normalized)
        localData.enqueueSync(
            entityType = SyncEntityType.LIVE_RESPONSE,
            entityId = normalized.id,
            payload = normalized
        )
    }

    private fun generateJoinCode(): String =
        buildString {
            repeat(4) { append(CHAR_POOL.random()) }
        }

    companion object {
        private val CHAR_POOL = ('A'..'Z') + ('0'..'9')
    }
}
