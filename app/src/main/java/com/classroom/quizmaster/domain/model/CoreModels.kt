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
 * Canonical student profile for students using Classroom Quiz Master.
 */
@Serializable
data class Student(
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
    val joinCode: String,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
    val isArchived: Boolean = false,
    val archivedAt: Instant? = null,
    val students: List<String> = emptyList()
)

/**
 * Represents a student's request to join a classroom.
 */
@Serializable
data class JoinRequest(
    val id: String,
    val studentId: String,
    val classroomId: String,
    val teacherId: String,
    val status: JoinRequestStatus,
    val createdAt: Instant,
    val resolvedAt: Instant? = null
)

@Serializable
enum class JoinRequestStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("approved")
    APPROVED,

    @SerialName("denied")
    DENIED
}


/**
 * Topics organize quizzes and assignments within a classroom. They can be
 * archived but are never hard deleted.
 */
@Serializable
data class Topic(
    val id: String,
    val classroomId: String,
    val teacherId: String,
    val name: String,
    val description: String = "",
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
    val isArchived: Boolean = false,
    val archivedAt: Instant? = null
)

/**
 * Quiz metadata including authored questions. The `questionCount` value defaults to the
 * size of [questions] but can be persisted independently for faster queries.
 */
@Serializable
data class Quiz(
    val id: String,
    val teacherId: String,
    val classroomId: String,
    val topicId: String,
    val title: String,
    val defaultTimePerQ: Int,
    val shuffle: Boolean,
    val createdAt: Instant,
    val category: QuizCategory = QuizCategory.STANDARD,
    val questions: List<Question> = emptyList(),
    val questionCount: Int = questions.size,
    val updatedAt: Instant = createdAt,
    val isArchived: Boolean = false,
    val archivedAt: Instant? = null
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

@Serializable
enum class QuizCategory {
    @SerialName("standard")
    STANDARD,

    @SerialName("pre")
    PRE_TEST,

    @SerialName("post")
    POST_TEST
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
 * Reference materials that can be authored by teachers and synced over LAN.
 */
@Serializable
data class LearningMaterial(
    val id: String,
    val teacherId: String,
    val classroomId: String,
    val classroomName: String = "",
    val topicId: String = "",
    val topicName: String = "",
    val title: String,
    val description: String = "",
    val body: String = "",
    val attachments: List<MaterialAttachment> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val isArchived: Boolean = false,
    val archivedAt: Instant? = null
)

/**
 * Attachment metadata describing the payload students should receive.
 */
@Serializable
data class MaterialAttachment(
    val id: String,
    val materialId: String,
    val displayName: String,
    val type: MaterialAttachmentType = MaterialAttachmentType.TEXT,
    val uri: String = "",
    val mimeType: String? = null,
    val sizeBytes: Long = 0,
    val downloadedAt: Instant? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class MaterialAttachmentType {
    @SerialName("text")
    TEXT,

    @SerialName("file")
    FILE,

    @SerialName("link")
    LINK,

    @SerialName("video")
    VIDEO
}

/**
 * Assignment metadata for asynchronous quiz delivery.
 */
@Serializable
data class Assignment(
    val id: String,
    val quizId: String,
    val classroomId: String,
    val topicId: String,
    val openAt: Instant,
    val closeAt: Instant,
    val attemptsAllowed: Int,
    val scoringMode: ScoringMode = ScoringMode.BEST,
    val revealAfterSubmit: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isArchived: Boolean = false,
    val archivedAt: Instant? = null
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
