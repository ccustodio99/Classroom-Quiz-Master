package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        sessionRepository.endSession()
        authRepository.logout()
    }
}
