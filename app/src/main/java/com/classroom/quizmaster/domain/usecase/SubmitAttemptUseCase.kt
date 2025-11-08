package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject

class SubmitAttemptUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
    private val sessionRepository: SessionRepository,
    private val scoreAttemptUseCase: ScoreAttemptUseCase
) {
    suspend operator fun invoke(
        session: Session,
        questionId: String,
        uid: String,
        selected: List<String>,
        nonce: String,
        timeTakenMs: Long,
        mirrorToCloud: Boolean = false
    ): Attempt {
        val quiz = quizRepository.getQuiz(session.quizId)
            ?: throw IllegalStateException("Quiz ${session.quizId} not found")
        val question = quiz.questions.firstOrNull { it.id == questionId }
            ?: throw IllegalArgumentException("Question $questionId not found in quiz ${quiz.id}")
        val timeLimitMs = question.timeLimitSeconds.coerceAtLeast(1) * 1_000L
        val attempt = scoreAttemptUseCase(
            uid = uid,
            questionId = questionId,
            selected = selected,
            correctAnswers = question.answerKey,
            timeTakenMs = timeTakenMs,
            timeLimitMs = timeLimitMs,
            nonce = nonce,
            revealHappened = session.reveal
        )
        sessionRepository.submitAttemptLocally(attempt)
        if (mirrorToCloud) {
            sessionRepository.mirrorAttempt(attempt)
        }
        return attempt
    }
}
