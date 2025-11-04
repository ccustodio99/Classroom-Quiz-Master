package com.acme.lms.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LiveSession(
    val id: String = "",
    val classId: String = "",
    val workId: String = "",
    val status: String = "created",
    val startedAt: Long? = null,
    val hostId: String = ""
)

@Serializable
data class LiveResponse(
    val id: String = "",
    val sessionId: String = "",
    val questionId: String = "",
    val userId: String = "",
    val payload: String = "",
    val ts: Long = 0L
)
