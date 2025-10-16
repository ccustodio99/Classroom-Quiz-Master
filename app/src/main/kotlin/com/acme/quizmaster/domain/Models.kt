package com.acme.quizmaster.domain

// From README.md and AGENTS.md

// --- Core Domain Models ---

data class Module(
    val id: String, // Added id for database
    val topic: String,
    val objectives: List<String>,
    val preTest: Assessment,
    val lesson: Lesson,
    val postTest: Assessment,
    val settings: ModuleSettings
)

data class Assessment(
    val id: String,
    val questions: List<Question>
)

data class Question(
    val id: String,
    val type: QuestionType,
    val objective: String,
    val stem: String, // The question text
    val options: List<String>? = null, // For MCQ
    val answer: String,
    val explanation: String? = null
)

enum class QuestionType {
    MCQ,
    TRUE_FALSE,
    NUMERIC,
    MATCHING
}

data class Lesson(
    val title: String,
    val slides: List<Slide>
)

data class Slide(
    val content: String,
    val mediaUri: String? = null
)

data class ModuleSettings(
    val retries: Int,
    val timeLimitMinutes: Int
)

// --- Agent-related Models ---

data class Violation(val field: String, val description: String)

data class SessionId(val id: String)

enum class JoinResult {
    SUCCESS,
    SESSION_FULL,
    NICKNAME_TAKEN,
    INVALID_SESSION
}

data class AnswerPayload(
    val questionId: String,
    val answer: String
)

enum class Ack {
    SUCCESS,
    FAILURE
}

data class LiveSnapshot(
    val sessionId: SessionId,
    val participants: List<Student>,
    val progress: Map<String, Float>, // studentId -> percentage complete
    val scores: Map<String, Int> // studentId -> score
)

data class Student(
    val id: String,
    val nickname: String
)

data class Attempt(
    val id: String,
    val assessmentId: String,
    val studentId: String,
    val answers: List<AnswerPayload>,
    val score: Scorecard? = null,
    val submittedAt: Long? = null
)

typealias AttemptId = String
typealias AssessmentId = String

data class Scorecard(
    val score: Int,
    val total: Int,
    val itemScores: Map<String, Boolean> // questionId -> correct/incorrect
)

data class ClassReport(
    val moduleId: String,
    val averagePreTest: Float,
    val averagePostTest: Float,
    val averageGain: Float,
    val objectiveMastery: Map<String, Float> // objective -> percentage mastery
)

data class StudentReport(
    val studentId: String,
    val moduleId: String,
    val preTestScore: Int,
    val postTestScore: Int,
    val scoreGain: Int,
    val objectiveMastery: Map<String, Float> // objective -> percentage mastery
)
