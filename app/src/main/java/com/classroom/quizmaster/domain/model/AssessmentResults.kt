package com.classroom.quizmaster.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Student(
    val id: String,
    val displayName: String
)

@Serializable
data class Attempt(
    val id: String,
    val moduleId: String,
    val assessmentId: String,
    val student: Student,
    val startedAt: Long,
    val submittedAt: Long? = null,
    val responses: List<AttemptItemResult> = emptyList()
)

@Serializable
data class AttemptItemResult(
    val itemId: String,
    val answer: String,
    val isCorrect: Boolean,
    val score: Double,
    val maxScore: Double
)

@Serializable
data class Scorecard(
    val attemptId: String,
    val totalScore: Double,
    val maxScore: Double,
    val percent: Double,
    val objectiveBreakdown: Map<String, ObjectiveScore>
)

@Serializable
data class ObjectiveScore(
    val objective: String,
    val earned: Double,
    val total: Double
)

@Serializable
data class ClassReport(
    val moduleId: String,
    val topic: String,
    val preAverage: Double,
    val postAverage: Double,
    val objectiveMastery: Map<String, ObjectiveMastery>,
    val attempts: List<AttemptSummary>
)

@Serializable
data class StudentReport(
    val moduleId: String,
    val student: Student,
    val preScore: Double,
    val postScore: Double,
    val mastery: Map<String, ObjectiveMastery>
)

@Serializable
data class ObjectiveMastery(
    val objective: String,
    val pre: Double,
    val post: Double
)

@Serializable
data class AttemptSummary(
    val student: Student,
    val prePercent: Double?,
    val postPercent: Double?
)

@Serializable
data class Assignment(
    val id: String,
    val moduleId: String,
    val dueEpochMs: Long,
    val submissions: List<AssignmentSubmission> = emptyList()
)

@Serializable
data class AssignmentSubmission(
    val student: Student,
    val attemptId: String,
    val submittedAt: Long
)

@Serializable
data class Badge(
    val id: String,
    val title: String,
    val description: String
)
