package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        quizId: String,
        classroomId: String,
        hostNickname: String
    ): Session {
        val quiz = quizRepository.getQuiz(quizId)
            ?: throw IllegalArgumentException("Quiz $quizId not found")
        val totalQuestions = quiz.questions.size.takeIf { it > 0 } ?: quiz.questionCount
        require(totalQuestions > 0) { "Cannot start a session without questions" }
        return sessionRepository.startLanSession(quizId, classroomId, hostNickname)
    }
}
