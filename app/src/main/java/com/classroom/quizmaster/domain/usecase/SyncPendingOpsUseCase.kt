package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject

class SyncPendingOpsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() = sessionRepository.syncPending()
}
