package com.classroom.quizmaster.ui.student.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.components.LeaderboardList
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.ConnectionQuality
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentLobbyUiState(
    val studentId: String = "",
    val hostName: String = "",
    val joinCode: String = "",
    val joinStatus: String = "Waiting for host",
    val players: List<PlayerLobbyUi> = emptyList(),
    val ready: Boolean = false,
    val lockedMessage: String? = null,
    val countdownSeconds: Int = 0,
    val leaderboardPreview: List<LeaderboardRowUi> = emptyList(),
    val connectionQuality: ConnectionQuality = ConnectionQuality.Good
)

@HiltViewModel
class StudentLobbyViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentLobbyUiState())
    val uiState: StateFlow<StudentLobbyUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentLobby.collectLatest { incoming ->
                _uiState.value = incoming
            }
        }
    }

    fun toggleReady() {
        val current = _uiState.value
        val next = !current.ready
        _uiState.update { it.copy(ready = next) }
        viewModelScope.launch {
            sessionRepositoryUi.toggleReady(current.studentId)
        }
    }
}

@Composable
fun StudentLobbyRoute(
    onReady: () -> Unit,
    viewModel: StudentLobbyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentLobbyScreen(
        state = state,
        onToggleReady = {
            viewModel.toggleReady()
            onReady()
        }
    )
}

@Composable
fun StudentLobbyScreen(
    state: StudentLobbyUiState,
    onToggleReady: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Host: ${state.hostName}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.joinStatus, style = MaterialTheme.typography.bodyMedium)
                if (state.lockedMessage != null) {
                    TagChip(text = state.lockedMessage)
                }
            }
            TagChip(text = "Join code ${state.joinCode}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip(text = connectionLabel(state.connectionQuality))
            if (state.countdownSeconds > 0) {
                TagChip(text = "Starts in ${state.countdownSeconds}s")
            }
        }
        Text("Players", style = MaterialTheme.typography.titleMedium)
        BoxWithConstraints {
            val isCompact = maxWidth < 480.dp
            if (state.players.isEmpty()) {
                Text("Waiting for classmates to join", style = MaterialTheme.typography.bodyMedium)
            } else {
                val columns = when {
                    maxWidth < 400.dp -> 1
                    maxWidth < 640.dp -> 2
                    else -> 3
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.players, key = { it.id }) { player ->
                        PlayerCard(
                            player = player,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (isCompact) 4f / 3f else 3f / 2f)
                        )
                    }
                }
            }
        }
        if (state.leaderboardPreview.isNotEmpty()) {
            LeaderboardList(
                rows = state.leaderboardPreview,
                modifier = Modifier.fillMaxWidth(),
                headline = "Top streaks",
                compact = true
            )
        }
        PrimaryButton(
            text = if (state.ready) "Ready!" else "Tap when ready",
            onClick = onToggleReady
        )
    }
}

@Composable
private fun PlayerCard(
    player: PlayerLobbyUi,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = if (player.ready) 6.dp else 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(player.nickname, style = MaterialTheme.typography.titleMedium)
            if (player.tag != null) {
                TagChip(text = player.tag)
            }
            Text(if (player.ready) "Ready" else "Getting set", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun connectionLabel(quality: ConnectionQuality): String = when (quality) {
    ConnectionQuality.Excellent -> "Connection excellent"
    ConnectionQuality.Good -> "Connection good"
    ConnectionQuality.Fair -> "Connection fair"
    ConnectionQuality.Weak -> "Connection weak"
    ConnectionQuality.Offline -> "Offline"
}

@QuizPreviews
@Composable
private fun StudentLobbyPreview() {
    QuizMasterTheme {
        StudentLobbyScreen(
            state = StudentLobbyUiState(
                studentId = "demo-student",
                hostName = "Ms. Navarro",
                joinCode = "SCI123",
                joinStatus = "Waiting for host",
                players = listOf(
                    PlayerLobbyUi("1", "Nova", AvatarOption("1", "N", emptyList(), "spark"), true, "You"),
                    PlayerLobbyUi("2", "Bolt", AvatarOption("2", "B", emptyList(), "atom"), false, "New")
                ),
                lockedMessage = "Host will lock after question 1",
                leaderboardPreview = listOf(
                    LeaderboardRowUi(1, "Nova", 1200, 25, AvatarOption("1", "N", emptyList(), "spark"), true),
                    LeaderboardRowUi(2, "Bolt", 980, 10, AvatarOption("2", "B", emptyList(), "atom"), false)
                )
            ),
            onToggleReady = {}
        )
    }
}
