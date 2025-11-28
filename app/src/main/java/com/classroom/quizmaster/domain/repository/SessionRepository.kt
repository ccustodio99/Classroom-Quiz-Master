package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.data.lan.LanDiscoveryEvent
import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.model.LanMeta
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val session: Flow<Session?>
    val participants: Flow<List<Participant>>
    val pendingOpCount: Flow<Int>
    val lanMeta: Flow<LanMeta?>

    suspend fun startLanSession(quizId: String, classroomId: String, hostNickname: String): Session
    suspend fun updateSessionState(session: Session)
    suspend fun submitAttemptLocally(attempt: Attempt)
    suspend fun mirrorAttempt(attempt: Attempt)
    fun discoverHosts(): Flow<LanDiscoveryEvent>
    suspend fun joinLanHost(service: LanServiceDescriptor, nickname: String): Result<Unit>
    suspend fun kickParticipant(uid: String)
    suspend fun syncPending()
    suspend fun endSession()
    suspend fun refreshCurrentSession()
}
