package com.classroom.quizmaster.ui.student

import com.classroom.quizmaster.data.lan.LanDiscoveryEvent
import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.data.lan.NearbyFallbackManager
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.LanMeta
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.model.SessionStatus
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.ui.student.join.StudentJoinViewModel
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class StudentJoinViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `discoverLanHosts updates services list`() = runTest(dispatcher) {
        val viewModel = StudentJoinViewModel(FakeSessionRepository(), NearbyFallbackManager())
        viewModel.discoverLanHosts()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.services.size)
        assertEquals("demo-ABC123", viewModel.uiState.value.services.first().serviceName)
    }

    private class FakeSessionRepository : SessionRepository {
        override val session: Flow<Session?> = MutableStateFlow(null)
        override val participants: Flow<List<Participant>> = MutableStateFlow(emptyList())
        override val pendingOpCount: Flow<Int> = MutableStateFlow(0)
        override val lanMeta: Flow<LanMeta?> = MutableStateFlow(null)

        override suspend fun startLanSession(quizId: String, classroomId: String, hostNickname: String): Session =
            Session(
                id = "s1",
                quizId = quizId,
                teacherId = "t1",
                classroomId = classroomId,
                joinCode = "ABC123",
                status = SessionStatus.LOBBY,
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
