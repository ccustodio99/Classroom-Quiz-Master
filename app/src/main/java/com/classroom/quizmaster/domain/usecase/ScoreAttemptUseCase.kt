package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.util.Idempotency
import com.classroom.quizmaster.util.ScoreCalculator
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreAttemptUseCase @Inject constructor() {

    operator fun invoke(
        uid: String,
        questionId: String,
        selected: List<String>,
        correctAnswers: List<String>,
        timeTakenMs: Long,
        timeLimitMs: Long,
        nonce: String,
        revealHappened: Boolean
    ): Attempt {
        val correct = selected.map(String::lowercase).sorted() == correctAnswers.map(String::lowercase).sorted()
        val points = ScoreCalculator.score(correct, timeLimitMs - timeTakenMs, timeLimitMs)
        return Attempt(
            id = Idempotency.attemptId(uid, questionId, nonce),
            uid = uid,
            questionId = questionId,
            selected = selected,
            timeMs = timeTakenMs,
            correct = correct,
            points = points,
            late = revealHappened,
            createdAt = Clock.System.now()
        )
    }
}
