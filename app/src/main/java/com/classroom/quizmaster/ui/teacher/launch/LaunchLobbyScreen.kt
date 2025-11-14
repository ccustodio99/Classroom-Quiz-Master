package com.classroom.quizmaster.ui.teacher.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.ConfirmEndDialog
import com.classroom.quizmaster.ui.components.ConfirmStartDialog
import com.classroom.quizmaster.ui.components.ConnectivityBanner
import com.classroom.quizmaster.ui.components.JoinCodeCard
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun LaunchLobbyRoute(
    onHostStarted: () -> Unit,
    onHostEnded: () -> Unit,
    viewModel: LaunchLobbyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchLobbyScreen(
        state = state,
        onToggleLeaderboard = viewModel::toggleLeaderboard,
        onToggleLock = viewModel::toggleLock,
        onStart = {
            viewModel.startHosting()
            onHostStarted()
        },
        onEnd = {
            viewModel.endHosting()
            onHostEnded()
        },
        onKick = viewModel::kick
    )
}

@Composable
fun LaunchLobbyScreen(
    state: LaunchLobbyUiState,
    onToggleLeaderboard: (Boolean) -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onKick: (String) -> Unit
) {
    var showStartDialog by rememberSaveable { mutableStateOf(false) }
    var showEndDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConnectivityBanner(
            headline = "LAN lobby ready",
            supportingText = "${state.discoveredPeers} peers nearby",
            statusChips = state.statusChips
        )
        state.snackbarMessage
            ?.takeIf { it.isNotBlank() }
            ?.let { message ->
                TagChip(
                    text = message,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        JoinCodeCard(
            code = state.joinCode,
            expiresIn = state.qrSubtitle,
            peersConnected = state.discoveredPeers,
            onCopy = { /* TODO copy */ }
        )
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Lobby (${state.players.size})", style = MaterialTheme.typography.titleLarge)
                if (state.players.isEmpty()) {
                    TagChip(
                        text = "Waiting for students to join",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.players, key = { it.id }) { player ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(player.nickname, style = MaterialTheme.typography.titleMedium)
                                    player.tag?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                                TextButton(onClick = { onKick(player.id) }) { Text("Kick") }
                            }
                        }
                    }
                }
            }
        }
        SecondaryButton(
            text = if (state.hideLeaderboard) "Show leaderboard" else "Hide leaderboard",
            onClick = { onToggleLeaderboard(!state.hideLeaderboard) }
        )
        SecondaryButton(
            text = if (state.lockAfterFirst) "Unlock after Q1" else "Lock joins after Q1",
            onClick = { onToggleLock(!state.lockAfterFirst) }
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val buttonWidth = (maxWidth - 12.dp) / 2
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(text = "Start", onClick = { showStartDialog = true }, modifier = Modifier.width(buttonWidth))
                SecondaryButton(text = "End", onClick = { showEndDialog = true }, modifier = Modifier.width(buttonWidth))
            }
        }
    }

    ConfirmStartDialog(
        open = showStartDialog,
        onDismiss = { showStartDialog = false },
        onConfirm = {
            showStartDialog = false
            onStart()
        }
    )
    ConfirmEndDialog(
        open = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            onEnd()
        }
    )
}

@QuizPreviews
@Composable
private fun LaunchLobbyPreview() {
    QuizMasterTheme {
        LaunchLobbyScreen(
            state = LaunchLobbyUiState(
                joinCode = "R7FT",
                qrSubtitle = "09:12",
                discoveredPeers = 6,
                statusChips = listOf(
                    StatusChipUi("lan", "LAN", StatusChipType.Lan),
                    StatusChipUi("cloud", "Cloud", StatusChipType.Cloud)
                ),
                players = listOf(
                    PlayerLobbyUi("1", "Kai", AvatarOption("1", "K", emptyList(), "spark"), true, "Ready"),
                    PlayerLobbyUi("2", "Lia", AvatarOption("2", "L", emptyList(), "atom"), false, "Waiting")
                )
            ),
            onToggleLeaderboard = {},
            onToggleLock = {},
            onStart = {},
            onEnd = {},
            onKick = {}
        )
    }
}
