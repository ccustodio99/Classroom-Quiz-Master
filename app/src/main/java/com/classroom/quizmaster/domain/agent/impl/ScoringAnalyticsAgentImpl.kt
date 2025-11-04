package com.classroom.quizmaster.domain.agent.impl

import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.domain.agent.ScoringAnalyticsAgent
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.MultipleChoiceQuestion
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.ShortAnswerQuestion
import com.classroom.quizmaster.domain.model.TrueFalseQuestion
import kotlin.math.roundToInt

class ScoringAnalyticsAgentImpl(
    private val localData: BlueprintLocalDataSource
) : ScoringAnalyticsAgent {

    override suspend fun calculateLearningGain(
        preTestAttempt: Attempt,
        postTestAttempt: Attempt
    ): Map<String, Float> {
        val preBundle = localData.findClasswork(preTestAttempt.assessmentId)
        val postBundle = localData.findClasswork(postTestAttempt.assessmentId)

        val preQuestions = preBundle?.questions.orEmpty()
        val postQuestions = postBundle?.questions.orEmpty()
        val comparableQuestions = postQuestions.takeIf { it.isNotEmpty() } ?: preQuestions

        val preScores = scoreAttempt(preTestAttempt, comparableQuestions)
        val postScores = scoreAttempt(postTestAttempt, comparableQuestions)

        if (comparableQuestions.isEmpty()) {
            return mapOf("overall" to (postScores.overall - preScores.overall))
        }

        val deltas = mutableMapOf<String, Float>()
        comparableQuestions.forEach { question ->
            val key = question.id
            val delta = (postScores.perQuestion[key] ?: 0f) - (preScores.perQuestion[key] ?: 0f)
            deltas[key] = delta
        }
        deltas["overall"] = postScores.overall - preScores.overall
        return deltas
    }

    private fun scoreAttempt(
        attempt: Attempt,
        questions: List<Question>
    ): AttemptScore {
        if (questions.isEmpty()) return AttemptScore.empty()
        var correctCount = 0
        val perQuestion = mutableMapOf<String, Float>()

        questions.forEach { question ->
            val answer = attempt.answers[question.id]
            val correct = evaluateQuestion(question, answer)
            if (correct) {
                correctCount += 1
                perQuestion[question.id] = 100f
            } else {
                perQuestion[question.id] = 0f
            }
        }
        val overall = (correctCount.toDouble() / questions.size.toDouble() * 100).roundToInt().toFloat()
        return AttemptScore(overall = overall, perQuestion = perQuestion)
    }

    private fun evaluateQuestion(question: Question, answerRaw: String?): Boolean {
        if (answerRaw.isNullOrBlank()) return false
        return when (question) {
            is MultipleChoiceQuestion -> {
                val userAnswers = answerRaw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                val correct = question.correctAnswers.toSet()
                userAnswers == correct
            }
            is TrueFalseQuestion -> {
                val normalized = answerRaw.trim().lowercase()
                val interpreted = when (normalized) {
                    "true", "1", "yes", "y" -> true
                    "false", "0", "no", "n" -> false
                    else -> return false
                }
                interpreted == question.correctAnswer
            }
            is ShortAnswerQuestion -> {
                val expected = question.correctAnswer.trim()
                val provided = answerRaw.trim()
                if (question.tolerance != null) {
                    val expectedNumber = expected.toDoubleOrNull()
                    val providedNumber = provided.toDoubleOrNull()
                    if (expectedNumber == null || providedNumber == null) {
                        false
                    } else {
                        kotlin.math.abs(expectedNumber - providedNumber) <= question.tolerance
                    }
                } else {
                    expected.equals(provided, ignoreCase = true)
                }
            }
            else -> false
        }
    }

    private data class AttemptScore(
        val overall: Float,
        val perQuestion: Map<String, Float>
    ) {
        companion object {
            fun empty() = AttemptScore(overall = 0f, perQuestion = emptyMap())
        }
    }
}
