package com.classroom.quizmaster.ui.teacher.launch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LaunchLobbyUiState(
    val joinCode: String = "----",
    val qrSubtitle: String = "",
    val qrPayload: String = "",
    val discoveredPeers: Int = 0,
    val players: List<PlayerLobbyUi> = emptyList(),
    val hideLeaderboard: Boolean = false,
    val lockAfterFirst: Boolean = false,
    val statusChips: List<StatusChipUi> = emptyList(),
    val snackbarMessage: String? = null
)

@HiltViewModel
class LaunchLobbyViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classroomId: String = savedStateHandle[CLASSROOM_ID_KEY]
        ?: throw IllegalArgumentException("classroomId is required")
    private val topicId: String? = savedStateHandle[TOPIC_ID_KEY]
    private val quizId: String? = savedStateHandle[QUIZ_ID_KEY]

    val uiState: StateFlow<LaunchLobbyUiState> = sessionRepositoryUi.launchLobby
        .map { it }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LaunchLobbyUiState()
        )

    init {
        require(classroomId.isNotBlank()) { "classroomId is required" }
        viewModelScope.launch {
            sessionRepositoryUi.configureHostContext(classroomId, topicId, quizId)
        }
    }

    fun toggleLeaderboard(hidden: Boolean) {
        viewModelScope.launch { sessionRepositoryUi.updateLeaderboardHidden(hidden) }
    }

    fun toggleLock(lock: Boolean) {
        viewModelScope.launch { sessionRepositoryUi.updateLockAfterFirst(lock) }
    }

    fun startHosting() {
        viewModelScope.launch { sessionRepositoryUi.startSession() }
    }

    fun endHosting() {
        viewModelScope.launch { sessionRepositoryUi.endSession() }
    }

    fun kick(uid: String) {
        viewModelScope.launch { sessionRepositoryUi.kickParticipant(uid) }
    }

    companion object {
        const val CLASSROOM_ID_KEY = "classroomId"
        const val TOPIC_ID_KEY = "topicId"
        const val QUIZ_ID_KEY = "quizId"
    }
}
