package com.example.lms.core.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String?,
    val role: UserRole,
    val org: String,
)

@Serializable
enum class UserRole { LEARNER, INSTRUCTOR, ADMIN }

@Serializable
data class Class(
    val id: String,
    val code: String = "",
    val section: String,
    val subject: String,
    val ownerId: String,
    val coTeachers: List<String> = emptyList(),
    val joinPolicy: JoinPolicy,
    val orgId: String = "", // Added
    val memberIds: List<String> = emptyList() // Added
)

@Serializable
enum class JoinPolicy { OPEN, REQUEST, INVITE_ONLY }

@Serializable
data class Roster(
    val id: String = "", // Added
    val classId: String,
    val userId: String,
    val role: RosterRole,
)

@Serializable
enum class RosterRole { LEARNER, CO_TEACHER, OWNER }

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
    val classworkId: String = "",
    val type: QuestionType = QuestionType.MCQ,
    val stem: String = "",
    val media: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val answerKey: String? = null,
    val config: Map<String, Any?> = emptyMap(),
    val points: Int = 1
)

@Serializable
enum class QuestionType {
    MCQ,
    TF,
    TYPE,
    PUZZLE,
    SLIDER,
    PIN,
    POLL,
    CLOUD,
    BRAINSTORM
}

@Serializable
data class Attempt(
    val id: String = "",
    val classId: String = "",
    val classworkId: String = "",
    val userId: String = "",
    val answers: Map<String, String> = emptyMap(),
    val score: Double? = null,
    val duration: Long? = null,
    val passed: Boolean? = null,
    val payload: Map<String, Any?> = emptyMap(),
    val startedAt: Long? = null,
    val submittedAt: Long? = null,
    val createdAt: Long? = null
)

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
data class LiveSession(
    val id: String,
    val classId: String,
    val assignmentId: String,
    val state: LiveSessionState,
    val startedAt: Long?,
    val endedAt: Long?,
)

@Serializable
enum class LiveSessionState { LOBBY, RUNNING, REVIEW }

@Serializable
data class LiveResponse(
    val sessionId: String,
    val userId: String,
    val questionId: String,
    val answerPayload: Map<String, Any?>,
    val correct: Boolean,
    val latencyMs: Long,
    val scoreDelta: Double,
    val timestamp: Long,
)

@Serializable
data class PresenceRecord(
    val classId: String,
    val userId: String,
    val updatedAt: Long,
    val expiresAt: Long,
)

@Serializable
enum class SignalType { OFFER, ANSWER, ICE }

@Serializable
data class LiveSignalMessage(
    val sessionId: String,
    val peerId: String,
    val type: SignalType,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val score: Double,
    val streak: Int,
    val latencyMs: Long,
)

sealed class LmsResult<out T> {
    data class Success<T>(val value: T) : LmsResult<T>()
    data class Error(val throwable: Throwable) : LmsResult<Nothing>()
}

@Serializable
data class SyncStatus(
    val inProgress: Boolean,
    val lastSuccessAt: Long?,
    val pendingItems: Int,
)

@Serializable
data class SeedClassroom(
    val classes: List<Class> = emptyList(),
    val roster: List<Roster> = emptyList(),
    val classwork: List<Classwork> = emptyList(),
    val questions: List<Question> = emptyList(),
)