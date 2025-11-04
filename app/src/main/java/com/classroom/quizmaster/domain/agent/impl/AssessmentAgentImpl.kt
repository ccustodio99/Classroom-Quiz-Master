package com.classroom.quizmaster.domain.agent.impl

import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.data.local.SyncEntityType
import com.classroom.quizmaster.domain.agent.AssessmentAgent
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.MultipleChoiceQuestion
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.ShortAnswerQuestion
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.model.TrueFalseQuestion
import java.util.UUID
import kotlin.math.roundToInt

class AssessmentAgentImpl(
    private val localData: BlueprintLocalDataSource
) : AssessmentAgent {

    override suspend fun start(classworkId: String, userId: String): Attempt {
        val attempt = Attempt(
            id = UUID.randomUUID().toString(),
            assessmentId = classworkId,
            userId = userId,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            answers = emptyMap()
        )
        localData.recordAttempt(attempt)
        localData.enqueueSync(
            entityType = SyncEntityType.ATTEMPT,
            entityId = attempt.id,
            payload = attempt
        )
        return attempt
    }

    override suspend fun submit(attempt: Attempt): Result<Submission> = runCatching {
        val completed = attempt.copy(
            completedAt = attempt.completedAt ?: System.currentTimeMillis()
        )
        localData.recordAttempt(completed)

        val bundle = localData.findClasswork(completed.assessmentId)
        val grade = bundle?.questions?.takeIf { it.isNotEmpty() }
            ?.let { questions ->
                val correct = questions.count { question ->
                    evaluateQuestion(question, completed.answers[question.id])
                }
                (correct.toDouble() / questions.size.toDouble() * 100).roundToInt()
            }?.toFloat()

        val submission = Submission(
            id = UUID.randomUUID().toString(),
            classworkId = completed.assessmentId,
            userId = completed.userId,
            submittedAt = completed.completedAt ?: System.currentTimeMillis(),
            grade = grade
        )

        localData.recordSubmission(submission)
        localData.enqueueSync(
            entityType = SyncEntityType.SUBMISSION,
            entityId = submission.id,
            payload = submission
        )

        submission
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
}
