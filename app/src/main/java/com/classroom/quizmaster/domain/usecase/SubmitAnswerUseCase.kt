package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject

class SubmitAnswerUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val scoreAttemptUseCase: ScoreAttemptUseCase
) {
    suspend operator fun invoke(
        uid: String,
        questionId: String,
        selected: List<String>,
        correctAnswers: List<String>,
        timeTakenMs: Long,
        timeLimitMs: Long,
        nonce: String,
        revealHappened: Boolean
    ) {
        val attempt = scoreAttemptUseCase(
            uid = uid,
            questionId = questionId,
            selected = selected,
            correctAnswers = correctAnswers,
            timeTakenMs = timeTakenMs,
            timeLimitMs = timeLimitMs,
            nonce = nonce,
            revealHappened = revealHappened
        )
        sessionRepository.submitAttemptLocally(attempt)
    }
}
