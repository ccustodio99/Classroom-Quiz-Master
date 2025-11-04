package com.classroom.quizmaster.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LiveSession(
    val id: String,
    val classworkId: String,
    val hostId: String,
    val joinCode: String,
    val status: SessionStatus = SessionStatus.LOBBY
)

@Serializable
enum class SessionStatus {
    LOBBY,
    IN_PROGRESS,
    FINISHED
}

@Serializable
data class LiveResponse(
    val sessionId: String,
    val userId: String,
    val questionId: String,
    val answer: String,
    val timestamp: Long
)
