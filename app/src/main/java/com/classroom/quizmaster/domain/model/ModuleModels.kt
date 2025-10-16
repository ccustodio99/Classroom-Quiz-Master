package com.classroom.quizmaster.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Module(
    val id: String,
    val subject: String = "G11 General Mathematics",
    val topic: String,
    val objectives: List<String>,
    val preTest: Assessment,
    val lesson: Lesson,
    val postTest: Assessment,
    val settings: ModuleSettings
)

@Serializable
data class ModuleSettings(
    val allowLeaderboard: Boolean = false,
    val revealAnswersAfterSection: Boolean = true,
    val timePerItemSeconds: Int = 60
)

@Serializable
data class Lesson(
    val id: String,
    val slides: List<LessonSlide>
)

@Serializable
data class LessonSlide(
    val id: String,
    val title: String,
    val content: String,
    val miniCheck: MiniCheck? = null
)

@Serializable
data class MiniCheck(
    val prompt: String,
    val correctAnswer: String
)

@Serializable
sealed interface Item {
    val id: String
    val objective: String
    val prompt: String
    val explanation: String
}

@Serializable
data class MultipleChoiceItem(
    override val id: String,
    override val objective: String,
    override val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    override val explanation: String
) : Item

@Serializable
data class TrueFalseItem(
    override val id: String,
    override val objective: String,
    override val prompt: String,
    val answer: Boolean,
    override val explanation: String
) : Item

@Serializable
data class NumericItem(
    override val id: String,
    override val objective: String,
    override val prompt: String,
    val answer: Double,
    val tolerance: Double = 0.01,
    override val explanation: String
) : Item

@Serializable
data class MatchingPair(
    val left: String,
    val right: String
)

@Serializable
data class MatchingItem(
    override val id: String,
    override val objective: String,
    override val prompt: String,
    val pairs: List<MatchingPair>,
    override val explanation: String
) : Item

@Serializable
data class Assessment(
    val id: String,
    val items: List<Item>,
    val timePerItemSec: Int = 60
)
