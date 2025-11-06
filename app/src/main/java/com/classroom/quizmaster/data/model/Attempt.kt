package com.classroom.quizmaster.data.model

data class Attempt(
    val id: String = "",
    val classId: String = "",
    val classworkId: String = "",
    val userId: String = "",
    val startedAt: Long = 0L,
    val submittedAt: Long? = null,
    val answers: Map<String, String> = emptyMap(),
    val score: Double? = null
)
