package com.classroom.quizmaster.ui.model

import androidx.compose.ui.graphics.Color

data class AvatarOption(
    val id: String,
    val label: String,
    val colors: List<Color>,
    val iconName: String
)

data class LeaderboardRowUi(
    val rank: Int,
    val displayName: String,
    val score: Int,
    val delta: Int,
    val avatar: AvatarOption,
    val isYou: Boolean = false
)

data class DistributionBar(
    val label: String,
    val value: Float,
    val isCorrect: Boolean = false
)

enum class QuestionTypeUi { MultipleChoice, TrueFalse, FillIn, Match }

data class AnswerOptionUi(
    val id: String,
    val label: String,
    val text: String,
    val correct: Boolean = false,
    val selected: Boolean = false,
    val percentage: Float = 0f
)

data class SelectionOptionUi(
    val id: String,
    val label: String,
    val supportingText: String = ""
)

enum class QuizCategoryUi(
    val displayName: String,
    val description: String,
    val routeValue: String
) {
    Standard("Standard quiz", "Use for regular classroom quizzes", "standard"),
    PreTest("Pre test", "Gauge readiness before a unit", "pre"),
    PostTest("Post test", "Measure growth after the unit", "post");

    companion object {
        fun fromRoute(value: String?): QuizCategoryUi = entries.firstOrNull { it.routeValue == value } ?: Standard
    }
}

data class QuestionDraftUi(
    val id: String,
    val stem: String,
    val type: QuestionTypeUi,
    val answers: List<AnswerOptionUi>,
    val explanation: String,
    val mediaThumb: String? = null,
    val timeLimitSeconds: Int = 45
)

data class QuizOverviewUi(
    val id: String,
    val title: String,
    val grade: String,
    val subject: String,
    val questionCount: Int,
    val averageScore: Int,
    val updatedAgo: String,
    val isDraft: Boolean,
    val classroomName: String = "",
    val topicName: String = ""
)

data class PlayerLobbyUi(
    val id: String,
    val nickname: String,
    val avatar: AvatarOption,
    val ready: Boolean,
    val tag: String? = null
)

enum class StatusChipType { Lan, Cloud, Offline }

data class StatusChipUi(
    val id: String,
    val label: String,
    val type: StatusChipType
)

data class AssignmentCardUi(
    val id: String,
    val title: String,
    val dueIn: String,
    val submissions: Int,
    val attemptsAllowed: Int,
    val statusLabel: String
)

data class ReportRowUi(
    val question: String,
    val pValue: Float,
    val topDistractor: String,
    val distractorRate: Float
)

enum class ConnectionQuality {
    Excellent,
    Good,
    Fair,
    Weak,
    Offline
}
