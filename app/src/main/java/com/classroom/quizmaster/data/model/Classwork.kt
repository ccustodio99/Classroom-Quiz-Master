package com.classroom.quizmaster.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Classwork(
    val id: String,
    val classId: String,
    val topic: String? = null,
    val type: ClassworkType,
    val title: String,
    val description: String? = null,
    val dueAt: Long? = null,
    val points: Int? = null
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
data class Submission(
    val id: String,
    val classworkId: String,
    val userId: String,
    val attemptId: String
)

@Serializable
data class Question(
    val id: String,
    val classworkId: String,
    val text: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val correctAnswer: String? = null
)

@Serializable
enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    TYPE_ANSWER,
    PUZZLE,
    SLIDER,
    PIN_DROP
}

@Serializable
data class Attempt(
    val id: String,
    val classworkId: String,
    val userId: String,
    val answers: Map<String, String> // Map of questionId to answer
)
