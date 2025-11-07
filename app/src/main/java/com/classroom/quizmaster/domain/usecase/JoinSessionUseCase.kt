package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.domain.repository.SessionRepository
import javax.inject.Inject

class JoinSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(descriptor: LanServiceDescriptor, nickname: String): Result<Unit> =
        sessionRepository.joinLanHost(descriptor, nickname)
}
