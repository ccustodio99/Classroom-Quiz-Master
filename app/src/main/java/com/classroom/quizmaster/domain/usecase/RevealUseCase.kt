package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject

class RevealUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(session: Session, reveal: Boolean = true): Session {
        if (session.reveal == reveal) {
            return session
        }
        val updated = session.copy(reveal = reveal)
        sessionRepository.updateSessionState(updated)
        return updated
    }
}
