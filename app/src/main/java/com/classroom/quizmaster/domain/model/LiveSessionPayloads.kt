package com.classroom.quizmaster.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AnswerPayload(
    val itemId: String,
    val answer: String,
    val studentId: String
)

@Serializable
data class LiveParticipant(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val score: Int = 0
)

@Serializable
data class LiveSnapshot(
    val sessionId: String,
    val moduleId: String? = null,
    val hostId: String? = null,
    val participants: List<LiveParticipant> = emptyList(),
    val answers: Map<String, List<AnswerPayload>> = emptyMap(),
    val activeItemId: String? = null,
    val activePrompt: String? = null,
    val activeObjective: String? = null,
    val updatedAt: Long = 0L
)
