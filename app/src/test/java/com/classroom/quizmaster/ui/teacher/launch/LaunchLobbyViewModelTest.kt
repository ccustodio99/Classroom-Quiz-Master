package com.classroom.quizmaster.ui.teacher.launch

import androidx.lifecycle.SavedStateHandle
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.ui.student.end.StudentEndUiState
import com.classroom.quizmaster.ui.student.entry.StudentEntryUiState
import com.classroom.quizmaster.ui.student.lobby.StudentLobbyUiState
import com.classroom.quizmaster.ui.student.play.StudentPlayUiState
import com.classroom.quizmaster.ui.student.play.SubmissionStatus
import com.classroom.quizmaster.ui.teacher.host.HostLiveUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LaunchLobbyViewModelTest {

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
    fun `startHosting delegates to repository`() = runTest(dispatcher) {
        val repository = FakeSessionRepositoryUi()
        val viewModel = createViewModel(repository)

        viewModel.startHosting()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.startCalls)
    }

    @Test
    fun `endHosting stops active session`() = runTest(dispatcher) {
        val repository = FakeSessionRepositoryUi()
        val viewModel = createViewModel(repository)

        viewModel.endHosting()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.endCalls)
    }

    private fun createViewModel(repositoryUi: FakeSessionRepositoryUi): LaunchLobbyViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                LaunchLobbyViewModel.CLASSROOM_ID_KEY to "class-1",
                LaunchLobbyViewModel.TOPIC_ID_KEY to null,
                LaunchLobbyViewModel.QUIZ_ID_KEY to null
            )
        )
        return LaunchLobbyViewModel(repositoryUi, savedStateHandle)
    }

    private class FakeSessionRepositoryUi : SessionRepositoryUi {
        private val launchFlow = MutableStateFlow(
            LaunchLobbyUiState(
                joinCode = "AAAA",
                qrSubtitle = "0.0.0.0",
                qrPayload = "ws://0.0.0.0:1234/ws?token=demo",
                discoveredPeers = 0,
                players = listOf(PlayerLobbyUi("host", "Host", avatar = null, ready = true, tag = "Host")),
                statusChips = listOf(StatusChipUi("lan", "LAN", StatusChipType.Lan))
            )
        )
        private val hostFlow = MutableStateFlow(HostLiveUiState())
        private val entryFlow = MutableStateFlow(StudentEntryUiState())
        private val lobbyFlow = MutableStateFlow(StudentLobbyUiState())
        private val playFlow = MutableStateFlow(
            StudentPlayUiState(submissionStatus = SubmissionStatus.Idle, submissionMessage = "Ready")
        )
        private val endFlow = MutableStateFlow(StudentEndUiState())

        var startCalls = 0
        var endCalls = 0
        var configuredContext: Triple<String, String?, String?>? = null

        override val launchLobby: Flow<LaunchLobbyUiState> = launchFlow.asStateFlow()
        override val hostState: Flow<HostLiveUiState> = hostFlow.asStateFlow()
        override val studentEntry: Flow<StudentEntryUiState> = entryFlow.asStateFlow()
        override val studentLobby: Flow<StudentLobbyUiState> = lobbyFlow.asStateFlow()
        override val studentPlay: Flow<StudentPlayUiState> = playFlow.asStateFlow()
        override val studentEnd: Flow<StudentEndUiState> = endFlow.asStateFlow()

        override suspend fun configureHostContext(classroomId: String, topicId: String?, quizId: String?) {
            configuredContext = Triple(classroomId, topicId, quizId)
        }

        override suspend fun updateLeaderboardHidden(hidden: Boolean) {}

        override suspend fun updateLockAfterFirst(lock: Boolean) {}

        override suspend fun updateMuteSfx(muted: Boolean) {}

        override suspend fun startSession() {
            startCalls++
        }

        override suspend fun endSession() {
            endCalls++
        }

        override suspend fun revealAnswer() {}

        override suspend fun nextQuestion() {}

        override suspend fun kickParticipant(uid: String) {}

        override suspend fun toggleReady(studentId: String) {}

        override suspend fun refreshLanHosts() {}

        override suspend fun joinLanHost(hostId: String, nickname: String, avatarId: String?): Result<Unit> =
            Result.failure(UnsupportedOperationException("not required for test"))

        override suspend fun joinWithCode(joinCode: String, nickname: String, avatarId: String?): Result<Unit> =
            Result.failure(UnsupportedOperationException("not required for test"))

        override suspend fun submitStudentAnswer(answerIds: List<String>) {}

        override suspend fun clearStudentError() {}
    }
}
