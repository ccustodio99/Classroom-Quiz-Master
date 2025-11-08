package com.classroom.quizmaster.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Canonical teacher profile for instructors using Classroom Quiz Master.
 */
@Serializable
data class Teacher(
    val id: String,
    val displayName: String,
    val email: String,
    val createdAt: Instant
)

/**
 * Represents a classroom owned by a teacher. Classrooms are used to scope quizzes,
 * live sessions, and assignments.
 */
@Serializable
data class Classroom(
    val id: String,
    val teacherId: String,
    val name: String,
    val grade: String,
    val subject: String,
    val createdAt: Instant
)

/**
 * Quiz metadata including authored questions. The `questionCount` value defaults to the
 * size of [questions] but can be persisted independently for faster queries.
 */
@Serializable
data class Quiz(
    val id: String,
    val teacherId: String,
    val title: String,
    val defaultTimePerQ: Int,
    val shuffle: Boolean,
    val createdAt: Instant,
    val questions: List<Question> = emptyList(),
    val questionCount: Int = questions.size,
    val updatedAt: Instant = createdAt
)

/**
 * Individual question within a quiz.
 */
@Serializable
data class Question(
    val id: String,
    val quizId: String,
    val type: QuestionType,
    val stem: String,
    val choices: List<String> = emptyList(),
    val answerKey: List<String> = emptyList(),
    val explanation: String = "",
    val media: MediaAsset? = null,
    val timeLimitSeconds: Int = 30
)

/**
 * Optional media asset associated with a question stem or explanation.
 */
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

/**
 * Active or historical live session metadata. Additional host specific values
 * such as [lanMeta] are maintained locally but never synced to Firestore.
 */
@Serializable
data class Session(
    val id: String,
    val quizId: String,
    val classroomId: String,
    val joinCode: String,
    val status: SessionStatus = SessionStatus.LOBBY,
    val currentIndex: Int = 0,
    val reveal: Boolean = false,
    val startedAt: Instant? = null,
    val lockAfterQ1: Boolean = false,
    val hideLeaderboard: Boolean = false,
    val teacherId: String = "",
    val endedAt: Instant? = null,
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

/**
 * Participant state tracked during a session.
 */
@Serializable
data class Participant(
    val uid: String,
    val nickname: String,
    val avatar: String,
    val totalPoints: Int = 0,
    val totalTimeMs: Long = 0,
    val joinedAt: Instant,
    val rank: Int = 0
)

/**
 * Attempt submitted for a question. Attempts are idempotent using the sha1 hash
 * of `uid|questionId|nonce`.
 */
@Serializable
data class Attempt(
    val id: String,
    val uid: String,
    val questionId: String,
    val selected: List<String> = emptyList(),
    val timeMs: Long,
    val correct: Boolean,
    val points: Int = 0,
    val late: Boolean,
    val createdAt: Instant
)

/**
 * Assignment metadata for asynchronous quiz delivery.
 */
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

/**
 * Aggregated submission per assignment/user.
 */
@Serializable
data class Submission(
    val uid: String,
    val assignmentId: String,
    val bestScore: Int = 0,
    val lastScore: Int = 0,
    val attempts: Int = 0,
    val updatedAt: Instant
)

/**
 * Local-only metadata for LAN hosting state.
 */
@Serializable
data class LanMeta(
    val sessionId: String,
    val token: String,
    val hostIp: String,
    val port: Int,
    val startedAt: Instant
)
