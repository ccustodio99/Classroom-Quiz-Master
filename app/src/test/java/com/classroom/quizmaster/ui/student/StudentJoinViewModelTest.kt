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
import com.classroom.quizmaster.util.NicknamePolicy
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

    @Test
    fun `join delegates to repository and clears joining flag`() = runTest(dispatcher) {
        val repository = FakeSessionRepository()
        val viewModel = StudentJoinViewModel(repository, NearbyFallbackManager())
        val descriptor = LanServiceDescriptor("demo-ABC123", "0.0.0.0", 8080, "ABC123", "ABC123", 0, "Demo Teacher")
        var successCount = 0

        viewModel.join(descriptor) { successCount++ }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.joinCalls)
        assertEquals(descriptor, repository.lastJoinDescriptor)
        val expectedNickname = NicknamePolicy.sanitize("Student", descriptor.joinCode + descriptor.timestamp)
        assertEquals(expectedNickname, repository.lastJoinNickname)
        assertEquals(1, successCount)
        assertEquals(false, viewModel.uiState.value.isJoining)
        assertEquals(null, viewModel.uiState.value.error)
    }

    private class FakeSessionRepository : SessionRepository {
        override val session: Flow<Session?> = MutableStateFlow(null)
        override val participants: Flow<List<Participant>> = MutableStateFlow(emptyList())
        override val pendingOpCount: Flow<Int> = MutableStateFlow(0)
        override val lanMeta: Flow<LanMeta?> = MutableStateFlow(null)
        var joinCalls = 0
        var lastJoinDescriptor: LanServiceDescriptor? = null
        var lastJoinNickname: String? = null

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
                    LanServiceDescriptor("demo-ABC123", "0.0.0.0", 8080, "ABC123", "ABC123", 0, "Demo Teacher")
                )
            )

        override suspend fun joinLanHost(service: LanServiceDescriptor, nickname: String): Result<Unit> {
            joinCalls++
            lastJoinDescriptor = service
            lastJoinNickname = nickname
            return Result.success(Unit)
        }
        override suspend fun kickParticipant(uid: String) {}
        override suspend fun syncPending() {}
        override suspend fun endSession() {}
    }
}
