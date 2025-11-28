package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.data.sync.PendingOpSyncer
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

class SyncPendingOpsUseCase @Inject constructor(
    private val pendingOpSyncer: PendingOpSyncer,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        val auth = authRepository.authState.firstOrNull()
        if (auth?.isAuthenticated != true) return
        pendingOpSyncer.syncPending()
        sessionRepository.syncPending()
    }
}
