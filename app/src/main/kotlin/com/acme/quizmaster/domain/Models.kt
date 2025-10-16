package com.acme.quizmaster.domain

import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class ItemType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    NUMERIC,
    MATCHING
}

data class Option(
    val id: String,
    val text: String
)

data class Violation(
    val field: String,
    val message: String
)

data class MatchingPair(
    val prompt: String,
    val answer: String
)

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val type: ItemType,
    val stem: String,
    val objective: String,
    val options: List<Option> = emptyList(),
    val answer: String,
    val explanation: String,
    val matchingPairs: List<MatchingPair> = emptyList(),
    val tolerance: Double = 0.0,
    val media: List<String> = emptyList()
)

data class Assessment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val items: List<Item>,
    val timeLimit: Duration = Duration.ofMinutes(10)
)

data class LessonSlide(
    val title: String,
    val body: String,
    val objective: String,
    val media: List<String> = emptyList()
)

data class Lesson(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val slides: List<LessonSlide>
)

data class ModuleSettings(
    val leaderboardEnabled: Boolean,
    val allowRetakes: Boolean,
    val locale: String,
    val feedbackMode: FeedbackMode
)

enum class FeedbackMode {
    AFTER_SECTION,
    END_OF_MODULE
}

data class Module(
    val id: String = UUID.randomUUID().toString(),
    val subject: String = "G11 General Mathematics",
    val topic: String,
    val objectives: List<String>,
    val preTest: Assessment,
    val lesson: Lesson,
    val postTest: Assessment,
    val settings: ModuleSettings
)

data class Student(
    val id: String = UUID.randomUUID().toString(),
    val nickname: String
)

data class AnswerPayload(
    val itemId: String,
    val response: String
)

data class Attempt(
    val id: String = UUID.randomUUID().toString(),
    val assessmentId: String,
    val moduleId: String,
    val studentId: String,
    val studentNickname: String,
    val startedAt: Instant = Instant.now(),
    val submittedAt: Instant? = null,
    val responses: List<AnswerPayload> = emptyList(),
    val score: Double = 0.0,
    val maxScore: Double = 0.0,
    val status: AttemptStatus = AttemptStatus.IN_PROGRESS
)

enum class AttemptStatus {
    IN_PROGRESS,
    SUBMITTED
}

data class Scorecard(
    val attemptId: String,
    val studentId: String,
    val studentNickname: String,
    val moduleId: String,
    val assessmentId: String,
    val score: Double,
    val maxScore: Double,
    val objectiveBreakdown: Map<String, ObjectiveScore>,
    val submittedAt: Instant
)

data class ObjectiveScore(
    val objective: String,
    val correct: Int,
    val total: Int
)

data class SessionSettings(
    val leaderboardEnabled: Boolean,
    val pace: SessionPace
)

enum class SessionPace {
    TEACHER_LED,
    STUDENT_PACED
}

data class ParticipantProgress(
    val studentId: String,
    val nickname: String,
    val answered: Int,
    val total: Int,
    val score: Double
)

data class LiveSnapshot(
    val sessionId: String,
    val moduleId: String,
    val participants: List<ParticipantProgress>
)

data class Assignment(
    val id: String = UUID.randomUUID().toString(),
    val moduleId: String,
    val startDate: Instant,
    val dueDate: Instant,
    val settings: AssignmentSettings,
    val submissions: MutableList<Attempt> = mutableListOf()
)

data class AssignmentSettings(
    val allowLateSubmissions: Boolean,
    val maxAttempts: Int
)

data class ClassReport(
    val moduleId: String,
    val topic: String,
    val preAssessmentId: String,
    val postAssessmentId: String,
    val preAverage: Double,
    val postAverage: Double,
    val objectives: List<ObjectiveGain>,
    val attempts: List<Scorecard>
)

data class StudentReport(
    val moduleId: String,
    val studentId: String,
    val nickname: String,
    val preScore: Double,
    val postScore: Double,
    val objectiveGains: List<ObjectiveGain>
)

data class ObjectiveGain(
    val objective: String,
    val pre: Double,
    val post: Double
)

data class GamificationBadge(
    val studentId: String,
    val nickname: String,
    val badge: String,
    val description: String
)
