package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.model.SessionStatus
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject

class AdvanceQuestionUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(current: Session): Session {
        val quiz = quizRepository.getQuiz(current.quizId)
            ?: throw IllegalStateException("Quiz ${current.quizId} not found")
        val totalQuestions = quiz.questions.size.takeIf { it > 0 } ?: quiz.questionCount
        require(totalQuestions > 0) { "Session has no questions to advance through" }

        val nextIndex = when {
            current.status == SessionStatus.LOBBY -> 0
            current.currentIndex < totalQuestions - 1 -> current.currentIndex + 1
            else -> throw IllegalStateException("No more questions to advance to")
        }
        val updated = current.copy(
            status = SessionStatus.ACTIVE,
            currentIndex = nextIndex,
            reveal = false
        )
        sessionRepository.updateSessionState(updated)
        return updated
    }
}
