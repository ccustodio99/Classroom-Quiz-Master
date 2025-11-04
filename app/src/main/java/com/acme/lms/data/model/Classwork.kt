package com.acme.lms.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Classwork(
    val id: String = "",
    val classId: String = "",
    val topic: String = "",
    val type: ClassworkType = ClassworkType.QUIZ,
    val title: String = "",
    val description: String = "",
    val dueAt: Long? = null,
    val points: Int = 0,
    val objectiveTags: List<String> = emptyList()
)

@Serializable
enum class ClassworkType {
    MATERIAL,
    QUIZ,
    PRETEST,
    POSTTEST,
    DISCUSSION,
    LIVE
}

@Serializable
data class Question(
    val id: String = "",
    val prompt: String = "",
    val type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val options: List<String> = emptyList(),
    val correctAnswer: String? = null,
    val points: Int = 1
)

@Serializable
enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    TYPE_ANSWER,
    PUZZLE,
    SLIDER,
    PIN_DROP,
    POLL
}

@Serializable
data class Submission(
    val id: String = "",
    val classId: String = "",
    val classworkId: String = "",
    val userId: String = "",
    val score: Int = 0,
    val attemptIds: List<String> = emptyList(),
    val updatedAt: Long = 0L
)

@Serializable
data class Attempt(
    val id: String = "",
    val classworkId: String = "",
    val userId: String = "",
    val answers: Map<String, String> = emptyMap(),
    val startedAt: Long? = null,
    val submittedAt: Long? = null
)
