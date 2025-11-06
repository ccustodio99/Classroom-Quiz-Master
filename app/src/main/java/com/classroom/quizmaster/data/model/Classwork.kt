package com.classroom.quizmaster.data.model

data class Classwork(
    val id: String = "",
    val classId: String = "",
    val title: String = "",
    val description: String = "",
    val type: ClassworkType = ClassworkType.ASSIGNMENT,
    val dueAt: Long? = null,
    val points: Int? = null,
    val maxAttempts: Int = 1
)

enum class ClassworkType { ASSIGNMENT, QUIZ, PRETEST, POSTTEST, LIVE }

data class Submission(
    val id: String = "",
    val classId: String = "",
    val classworkId: String = "",
    val userId: String = "",
    val attemptIds: List<String> = emptyList(),
    val score: Double? = null,
    val updatedAt: Long? = null
)
