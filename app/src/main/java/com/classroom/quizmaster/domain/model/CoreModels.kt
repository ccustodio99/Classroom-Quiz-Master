package com.classroom.quizmaster.domain.model

import kotlinx.datetime.Instant
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

enum class MediaType { IMAGE, AUDIO, VIDEO }

enum class QuestionType { MCQ, TF, FILL_IN, MATCHING }

@Serializable
data class Session(
    val id: String,
    val quizId: String,
    val teacherId: String,
    val classroomId: String,
    val joinCode: String,
    val status: SessionStatus,
    val currentIndex: Int,
    val reveal: Boolean,
    val startedAt: Instant? = null,
    val lockAfterQ1: Boolean = false,
    val hideLeaderboard: Boolean = false
)

enum class SessionStatus { LOBBY, ACTIVE, ENDED }

@Serializable
data class Participant(
    val uid: String,
    val nickname: String,
    val avatar: String,
    val totalPoints: Int,
    val totalTimeMs: Long,
    val joinedAt: Instant
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
    val revealAfterSubmit: Boolean
)

@Serializable
data class Submission(
    val uid: String,
    val assignmentId: String,
    val bestScore: Int,
    val lastScore: Int,
    val attempts: Int,
    val updatedAt: Instant
)

data class LanMeta(
    val sessionId: String,
    val token: String,
    val hostIp: String,
    val port: Int,
    val startedAt: Instant
)
