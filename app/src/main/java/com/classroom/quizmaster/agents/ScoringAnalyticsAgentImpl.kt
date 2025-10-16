package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.AttemptRepository
import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.AttemptSummary
import com.classroom.quizmaster.domain.model.ClassReport
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.ObjectiveMastery
import com.classroom.quizmaster.domain.model.Scorecard
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
            student = studentAttempts.first().student,
            preScore = summary.prePercent ?: 0.0,
            postScore = summary.postPercent ?: 0.0,
            mastery = mastery
        )
    }

    private fun summarizeStudent(module: Module, attempts: List<Attempt>): AttemptSummary? {
        if (attempts.isEmpty()) return null
        val pre = attempts.find { it.assessmentId == module.preTest.id }
        val post = attempts.find { it.assessmentId == module.postTest.id }
        val preScore = pre?.responses?.sumOf { it.score }?.let { it / (pre.responses.sumOf { result -> result.maxScore }) * 100 }
        val postScore = post?.responses?.sumOf { it.score }?.let { it / (post.responses.sumOf { result -> result.maxScore }) * 100 }
        return AttemptSummary(
            student = attempts.first().student,
            prePercent = preScore,
            postPercent = postScore
        )
    }

    private fun computeMastery(module: Module, attempts: List<Attempt>): Map<String, ObjectiveMastery> {
        val objectives = module.objectives
        val preMap = mutableMapOf<String, MutableList<Double>>()
        val postMap = mutableMapOf<String, MutableList<Double>>()
        attempts.forEach { attempt ->
            val objectiveScores = attempt.responses.map { response ->
                val item = (module.preTest.items + module.postTest.items).firstOrNull { it.id == response.itemId }
                item?.objective to response
            }.filter { it.first != null }
            val map = if (attempt.assessmentId == module.preTest.id) preMap else postMap
            objectiveScores.forEach { (objective, result) ->
                val percent = if (result!!.maxScore == 0.0) 0.0 else (result.score / result.maxScore) * 100
                map.getOrPut(objective!!) { mutableListOf() } += percent
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
