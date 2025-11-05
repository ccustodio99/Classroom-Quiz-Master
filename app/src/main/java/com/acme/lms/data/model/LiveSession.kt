package com.acme.lms.data.model

data class LiveSession(
    val id: String = "",
    val classId: String = "",
    val assignmentId: String = "",
    val state: LiveSessionState = LiveSessionState.LOBBY,
    val startedAt: Long? = null,
    val endedAt: Long? = null
)

enum class LiveSessionState { LOBBY, RUNNING, ENDED }

data class LiveResponse(
    val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val answer: String = "",
    val timestamp: Long = 0L
)
