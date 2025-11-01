package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.AttemptRepository
import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.AttemptSummary
import com.classroom.quizmaster.domain.model.ClassReport
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.ObjectiveMastery
import com.classroom.quizmaster.domain.model.AttemptItemResult
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.StudentReport
import kotlinx.coroutines.flow.first

class ScoringAnalyticsAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val attemptRepository: AttemptRepository
) : ScoringAnalyticsAgent {
    override suspend fun buildClassReport(moduleId: String): ClassReport {
        val module = moduleRepository.getModule(moduleId) ?: error("Module missing")
        val attempts = attemptRepository.observeByModule(moduleId).first()
        val grouped = attempts.groupBy { it.student.id }
        val summaries = grouped.mapNotNull { (_, list) -> summarizeStudent(module, list) }
        val preScores = summaries.mapNotNull { it.prePercent }
        val postScores = summaries.mapNotNull { it.postPercent }
        val objectiveMastery = computeMastery(module, attempts)
        return ClassReport(
            moduleId = module.id,
            topic = module.topic,
            preAverage = preScores.averageOrZero(),
            postAverage = postScores.averageOrZero(),
            objectiveMastery = objectiveMastery,
            attempts = summaries
        )
    }

    override suspend fun buildStudentReport(moduleId: String, studentId: String): StudentReport {
        val module = moduleRepository.getModule(moduleId) ?: error("Module missing")
        val attempts = attemptRepository.observeByModule(moduleId).first()
        val studentAttempts = attempts.filter { it.student.id == studentId }
        val summary = summarizeStudent(module, studentAttempts) ?: error("No attempts")
        val mastery = computeMastery(module, studentAttempts)
        return StudentReport(
            moduleId = moduleId,
            student = summary.student,
            preScore = summary.prePercent ?: 0.0,
            postScore = summary.postPercent ?: 0.0,
            mastery = mastery
        )
    }

    private fun summarizeStudent(module: Module, attempts: List<Attempt>): AttemptSummary? {
        if (attempts.isEmpty()) return null
        val pre = attempts.latestForAssessment(module.preTest.id)
        val post = attempts.latestForAssessment(module.postTest.id)
        val preScore = pre?.responses?.percentOrNull()
        val postScore = post?.responses?.percentOrNull()
        return AttemptSummary(
            student = attempts.mostRecentStudent(),
            prePercent = preScore,
            postPercent = postScore
        )
    }

    private fun computeMastery(module: Module, attempts: List<Attempt>): Map<String, ObjectiveMastery> {
        val objectives = module.objectives
        val preMap = mutableMapOf<String, MutableList<Double>>()
        val postMap = mutableMapOf<String, MutableList<Double>>()
        val itemsById = (module.preTest.items + module.postTest.items).associateBy { it.id }
        val latestAttempts = attempts
            .groupBy { it.student.id to it.assessmentId }
            .mapNotNull { (_, list) -> list.maxByOrNull { it.sortTimestamp() } }
        latestAttempts.forEach { attempt ->
            val bucket = if (attempt.assessmentId == module.preTest.id) preMap else postMap
            attempt.responses.forEach { response ->
                val objective = itemsById[response.itemId]?.objective ?: return@forEach
                val percent = response.percentScore()
                bucket.getOrPut(objective) { mutableListOf() } += percent
            }
        }
        return objectives.associateWith { objective ->
            ObjectiveMastery(
                objective = objective,
                pre = preMap[objective]?.averageOrZero() ?: 0.0,
                post = postMap[objective]?.averageOrZero() ?: 0.0
            )
        }
    }
}

private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

private fun List<AttemptItemResult>?.percentOrNull(): Double? {
    if (this == null || isEmpty()) return null
    val total = sumOf { it.maxScore }
    if (total <= 0.0) return null
    val earned = sumOf { it.score }
    return (earned / total) * 100.0
}

private fun AttemptItemResult.percentScore(): Double =
    if (maxScore <= 0.0) 0.0 else (score / maxScore) * 100.0

private fun List<Attempt>.latestForAssessment(assessmentId: String): Attempt? =
    filter { it.assessmentId == assessmentId }
        .maxByOrNull { it.sortTimestamp() }

private fun List<Attempt>.mostRecentStudent(): Student =
    maxByOrNull { it.sortTimestamp() }?.student ?: first().student

private fun Attempt.sortTimestamp(): Long = submittedAt ?: startedAt

