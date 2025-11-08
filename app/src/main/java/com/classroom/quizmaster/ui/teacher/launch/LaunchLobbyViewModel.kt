package com.classroom.quizmaster.ui.teacher.launch

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
    val discoveredPeers: Int = 0,
    val players: List<PlayerLobbyUi> = emptyList(),
    val hideLeaderboard: Boolean = false,
    val lockAfterFirst: Boolean = false,
    val statusChips: List<StatusChipUi> = emptyList(),
    val snackbarMessage: String? = null
)

@HiltViewModel
class LaunchLobbyViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {

    val uiState: StateFlow<LaunchLobbyUiState> = sessionRepositoryUi.launchLobby
        .map { it }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LaunchLobbyUiState()
        )

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
}
