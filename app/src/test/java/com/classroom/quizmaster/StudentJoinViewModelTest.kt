package com.classroom.quizmaster

import com.classroom.quizmaster.data.lan.LanDiscoveryEvent
import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.data.lan.NearbyFallbackManager
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.LanMeta
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.ui.student.join.StudentJoinViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudentJoinViewModelTest {

    @Test
    fun `discover updates services`() = runTest {
        val repo = FakeSessionRepository()
        val viewModel = StudentJoinViewModel(repo, NearbyFallbackManager())
        viewModel.discoverLanHosts()
        assertEquals(1, viewModel.uiState.value.services.size)
    }

    private class FakeSessionRepository : SessionRepository {
        override val session: Flow<Session?> = MutableStateFlow(null)
        override val participants: Flow<List<Participant>> = MutableStateFlow(emptyList())
        override val pendingOpCount: Flow<Int> = MutableStateFlow(0)
        override val lanMeta: Flow<LanMeta?> = MutableStateFlow(null)

        override suspend fun startLanSession(quizId: String, classroomId: String, hostNickname: String): Session =
            Session(
                id = "session",
                quizId = quizId,
                teacherId = "teacher",
                classroomId = classroomId,
                joinCode = "ABC123",
                status = com.classroom.quizmaster.domain.model.SessionStatus.LOBBY,
                currentIndex = 0,
                reveal = false
            )

        override suspend fun updateSessionState(session: Session) {}

        override suspend fun submitAttemptLocally(attempt: Attempt) {}

        override suspend fun mirrorAttempt(attempt: Attempt) {}

        override fun discoverHosts(): Flow<LanDiscoveryEvent> =
            flowOf(
                LanDiscoveryEvent.ServiceFound(
                    LanServiceDescriptor("demo-ABC123", "0.0.0.0", 8080, "ABC123", "ABC123", 0)
                )
            )

        override suspend fun joinLanHost(service: LanServiceDescriptor, nickname: String) = Result.success(Unit)

        override suspend fun kickParticipant(uid: String) {}

        override suspend fun syncPending() {}

        override suspend fun endSession() {}
    }
}
