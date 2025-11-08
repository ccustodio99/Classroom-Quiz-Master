package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.model.SessionStatus
import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.datetime.Clock

class EndSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(session: Session): Session {
        val ended = session.copy(
            status = SessionStatus.ENDED,
            reveal = false,
            endedAt = Clock.System.now()
        )
        sessionRepository.updateSessionState(ended)
        sessionRepository.endSession()
        return ended
    }
}
