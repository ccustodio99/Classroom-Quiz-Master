package com.classroom.quizmaster.ui.teacher.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.model.DistributionBar
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HostLiveUiState(
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val timerSeconds: Int = 45,
    val isRevealed: Boolean = false,
    val question: QuestionDraftUi? = null,
    val distribution: List<DistributionBar> = emptyList(),
    val leaderboard: List<LeaderboardRowUi> = emptyList(),
    val muteSfx: Boolean = true,
    val showLeaderboard: Boolean = true,
    val joinCode: String = "",
    val lanEndpoint: String = ""
)

@HiltViewModel
class HostLiveViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {

    val uiState: StateFlow<HostLiveUiState> = sessionRepositoryUi.hostState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HostLiveUiState())

    fun reveal() {
        viewModelScope.launch { sessionRepositoryUi.revealAnswer() }
    }

    fun next() {
        viewModelScope.launch { sessionRepositoryUi.nextQuestion() }
    }

    fun toggleLeaderboard(show: Boolean) {
        viewModelScope.launch { sessionRepositoryUi.updateLeaderboardHidden(!show) }
    }

    fun toggleMute(muted: Boolean) {
        viewModelScope.launch { sessionRepositoryUi.updateMuteSfx(muted) }
    }

    fun endSession() {
        viewModelScope.launch { sessionRepositoryUi.endSession() }
    }
}
