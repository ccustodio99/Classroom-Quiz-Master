package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.AttemptRepository
import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.AttemptItemResult
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.ObjectiveScore
import com.classroom.quizmaster.domain.model.Scorecard
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.TrueFalseItem
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AssessmentAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val attemptRepository: AttemptRepository
) : AssessmentAgent {

    private val activeAttempts = mutableMapOf<String, Attempt>()
    private val mutex = Mutex()

    override suspend fun start(assessmentId: String, student: Student): String {
        val module = moduleRepository.findModuleByAssessment(assessmentId)
            ?: error("Assessment not found")
        val attemptId = UUID.randomUUID().toString()
        val attempt = Attempt(
            id = attemptId,
            moduleId = module.id,
            assessmentId = assessmentId,
            student = student,
            startedAt = System.currentTimeMillis()
        )
        mutex.withLock { activeAttempts[attemptId] = attempt }
        attemptRepository.upsert(attempt)
        return attemptId
    }

    override suspend fun submit(attemptId: String, answers: List<AnswerPayload>): Scorecard {
        val attempt = mutex.withLock { activeAttempts.remove(attemptId) }
            ?: attemptRepository.find(attemptId)
            ?: error("Attempt not found")
        val module = moduleRepository.getModule(attempt.moduleId) ?: error("Module missing")
        val assessment = when (attempt.assessmentId) {
            module.preTest.id -> module.preTest
            module.postTest.id -> module.postTest
            else -> error("Assessment not part of module")
        }
        val results = assessment.items.map { item ->
            val answer = answers.firstOrNull { it.itemId == item.id }?.answer ?: ""
            evaluate(item, answer)
        }
        val total = results.sumOf { it.score }
        val max = results.sumOf { it.maxScore }
        val breakdown = assessment.items
            .zip(results)
            .groupBy({ it.first.objective }, { it.second })
            .mapValues { entry ->
                ObjectiveScore(
                    objective = entry.key,
                    earned = entry.value.sumOf { it.score },
                    total = entry.value.sumOf { it.maxScore }
                )
            }
        val updated = attempt.copy(
            submittedAt = System.currentTimeMillis(),
            responses = results
        )
        attemptRepository.upsert(updated)
        return Scorecard(
            attemptId = attemptId,
            totalScore = total,
            maxScore = max,
            percent = if (max == 0.0) 0.0 else (total / max) * 100.0,
            objectiveBreakdown = breakdown
        )
    }

    private fun evaluate(item: com.classroom.quizmaster.domain.model.Item, answer: String): AttemptItemResult {
        val (isCorrect, earned) = when (item) {
            is MultipleChoiceItem -> {
                val selected = answer.toIntOrNull()
                val correct = selected == item.correctIndex
                correct to if (correct) 1.0 else 0.0
            }
            is TrueFalseItem -> {
                val normalized = answer.trim().lowercase()
                val correct = (normalized == "true" && item.answer) || (normalized == "false" && !item.answer)
                correct to if (correct) 1.0 else 0.0
            }
            is NumericItem -> {
                val value = answer.toDoubleOrNull()
                val correct = value?.let { abs(it - item.answer) <= item.tolerance } == true
                correct to if (correct) 1.0 else 0.0
            }
            is MatchingItem -> {
                val tokens = answer.split(";").map { it.trim() }.filter { it.contains("->") }
                val correctPairs = item.pairs.map { "${it.left}->${it.right}" }.toSet()
                val earned = if (item.pairs.isEmpty()) 0.0 else tokens.count { it in correctPairs }.toDouble() / item.pairs.size
                (earned == 1.0) to earned
            }
        }
        return AttemptItemResult(
            itemId = item.id,
            answer = answer,
            isCorrect = isCorrect,
            score = earned,
            maxScore = 1.0
        )
    }
}
