package com.classroom.quizmaster.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Module(
    val id: String,
    val classroom: ClassroomProfile = ClassroomProfile(),
    val subject: String = classroom.subject,
    val topic: String,
    val objectives: List<String>,
    val preTest: Assessment,
    val lesson: Lesson,
    val postTest: Assessment,
    val settings: ModuleSettings
)

@Serializable
data class ClassroomProfile(
    val id: String = "classroom-default",
    val name: String = "Advisory Class",
    val subject: String = "G11 General Mathematics",
    val description: String = "",
    val gradeLevel: String = "Grade 11",
    val section: String = ""
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
    val slides: List<LessonSlide>,
    val interactiveActivities: List<InteractiveActivity> = emptyList()
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
    val correctAnswer: String,
    val objectives: List<String> = emptyList()
)

@Serializable
sealed interface InteractiveActivity {
    val id: String
    val title: String
    val prompt: String
    val isScored: Boolean
}

@Serializable
@kotlinx.serialization.SerialName("quiz")
data class QuizActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val options: List<String>,
    val correctAnswers: List<Int>,
    val allowMultiple: Boolean = false,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("true_false")
data class TrueFalseActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val correctAnswer: Boolean,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("type_answer")
data class TypeAnswerActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val correctAnswer: String,
    val maxCharacters: Int = 20,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("puzzle")
data class PuzzleActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val blocks: List<String>,
    val correctOrder: List<String>,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("slider")
data class SliderActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val minValue: Int,
    val maxValue: Int,
    val target: Int,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("poll")
data class PollActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val options: List<String>,
    override val isScored: Boolean = false
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("word_cloud")
data class WordCloudActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val maxWords: Int = 1,
    val maxCharacters: Int = 16,
    override val isScored: Boolean = false
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("open_ended")
data class OpenEndedActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val maxCharacters: Int = 240,
    override val isScored: Boolean = false
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("brainstorm")
data class BrainstormActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val categories: List<String>,
    val voteLimit: Int = 2,
    override val isScored: Boolean = false
) : InteractiveActivity

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
