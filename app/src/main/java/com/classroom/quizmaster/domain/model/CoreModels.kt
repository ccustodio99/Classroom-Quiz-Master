package com.classroom.quizmaster.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Teacher(
    val id: String,
    val displayName: String,
    val email: String,
    val createdAt: Instant
)

@Serializable
data class Classroom(
    val id: String,
    val teacherId: String,
    val name: String,
    val grade: String,
    val subject: String,
    val createdAt: Instant
)

@Serializable
data class Quiz(
    val id: String,
    val teacherId: String,
    val title: String,
    val defaultTimePerQ: Int,
    val shuffle: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
    val questionCount: Int = 0,
    val questions: List<Question> = emptyList()
)

@Serializable
data class Question(
    val id: String,
    val quizId: String,
    val type: QuestionType,
    val stem: String,
    val choices: List<String>,
    val answerKey: List<String>,
    val explanation: String,
    val media: MediaAsset? = null,
    val timeLimitSeconds: Int = 30
)

@Serializable
data class MediaAsset(
    val type: MediaType,
    val url: String
)

@Serializable
enum class MediaType {
    @SerialName("image")
    IMAGE,

    @SerialName("audio")
    AUDIO,

    @SerialName("video")
    VIDEO
}

@Serializable
enum class QuestionType {
    @SerialName("mcq")
    MCQ,

    @SerialName("tf")
    TF,

    @SerialName("fillin")
    FILL_IN,

    @SerialName("matching")
    MATCHING
}

@Serializable
data class Session(
    val id: String,
    val quizId: String,
    val teacherId: String,
    val classroomId: String,
    val joinCode: String,
    val status: SessionStatus = SessionStatus.LOBBY,
    val currentIndex: Int = 0,
    val reveal: Boolean = false,
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val lockAfterQ1: Boolean = false,
    val hideLeaderboard: Boolean = false,
    val lanMeta: LanMeta? = null
)

@Serializable
enum class SessionStatus {
    @SerialName("lobby")
    LOBBY,

    @SerialName("active")
    ACTIVE,

    @SerialName("ended")
    ENDED
}

@Serializable
data class Participant(
    val uid: String,
    val nickname: String,
    val avatar: String,
    val totalPoints: Int,
    val totalTimeMs: Long,
    val joinedAt: Instant,
    val rank: Int = 0
)

@Serializable
data class Attempt(
    val id: String,
    val uid: String,
    val questionId: String,
    val selected: List<String>,
    val timeMs: Long,
    val correct: Boolean,
    val points: Int,
    val late: Boolean,
    val createdAt: Instant
)

@Serializable
data class Assignment(
    val id: String,
    val quizId: String,
    val classroomId: String,
    val openAt: Instant,
    val closeAt: Instant,
    val attemptsAllowed: Int,
    val scoringMode: ScoringMode = ScoringMode.BEST,
    val revealAfterSubmit: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class ScoringMode {
    @SerialName("best")
    BEST,

    @SerialName("last")
    LAST,

    @SerialName("avg")
    AVERAGE
}

@Serializable
data class Submission(
    val uid: String,
    val assignmentId: String,
    val bestScore: Int,
    val lastScore: Int,
    val attempts: Int,
    val updatedAt: Instant
)

@Serializable
data class LanMeta(
    val sessionId: String,
    val token: String,
    val hostIp: String,
    val port: Int,
    val startedAt: Instant
)
