package com.example.lms.core.model

data class User(
    val id: String,
    val name: String,
    val email: String?,
    val role: UserRole,
    val org: String,
)

enum class UserRole { LEARNER, INSTRUCTOR, ADMIN }

data class Class(
    val id: String,
    val code: String,
    val section: String,
    val subject: String,
    val ownerId: String,
    val coTeachers: List<String>,
    val joinPolicy: JoinPolicy,
)

enum class JoinPolicy { OPEN, REQUEST, INVITE_ONLY }

data class Roster(
    val classId: String,
    val userId: String,
    val role: RosterRole,
)

enum class RosterRole { LEARNER, CO_TEACHER, OWNER }

data class Classwork(
    val id: String,
    val classId: String,
    val topic: String,
    val title: String,
    val type: ClassworkType,
    val dueAt: Long?,
    val points: Int?,
)

enum class ClassworkType {
    MATERIAL, QUIZ, PRETEST, POSTTEST, DISCUSSION, LIVE
}

data class Question(
    val id: String,
    val classworkId: String,
    val type: QuestionType,
    val stem: String,
    val media: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val answerKey: String? = null,
    val config: Map<String, Any?> = emptyMap(),
)

enum class QuestionType {
    MCQ, TF, TYPE, PUZZLE, SLIDER, PIN, POLL, CLOUD, BRAINSTORM
}

data class Attempt(
    val id: String,
    val userId: String,
    val classworkId: String,
    val score: Double,
    val duration: Long,
    val passed: Boolean,
    val payload: Map<String, Any?>,
    val createdAt: Long,
)

data class LiveSession(
    val id: String,
    val classId: String,
    val assignmentId: String,
    val state: LiveSessionState,
    val startedAt: Long?,
    val endedAt: Long?,
)

enum class LiveSessionState { LOBBY, RUNNING, REVIEW }

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

data class PresenceRecord(
    val classId: String,
    val userId: String,
    val updatedAt: Long,
    val expiresAt: Long,
)

enum class SignalType { OFFER, ANSWER, ICE }

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

data class SyncStatus(
    val inProgress: Boolean,
    val lastSuccessAt: Long?,
    val pendingItems: Int,
)

data class SeedClassroom(
    val classes: List<Class> = emptyList(),
    val roster: List<Roster> = emptyList(),
    val classwork: List<Classwork> = emptyList(),
    val questions: List<Question> = emptyList(),
)

