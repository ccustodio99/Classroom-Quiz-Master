package com.classroom.quizmaster.ui.teacher.launch

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.R
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
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copyConfirmation = stringResource(R.string.launch_lobby_copy_confirmation)
    val scanInstructions = stringResource(R.string.launch_lobby_scan_instructions)
    val blockingMessage = state.snackbarMessage
        ?.takeIf { it.isNotBlank() && state.joinCode == "----" && state.qrPayload.isBlank() }
    val controlsEnabled = blockingMessage == null
    val scrollState = rememberScrollState()
    val headline = if (state.joinCode == "----" && state.qrPayload.isBlank()) {
        "Prepare lobby"
    } else {
        "LAN lobby ready"
    }
    val supporting = when {
        blockingMessage != null -> blockingMessage
        state.joinCode == "----" -> "Tap Start to generate a join code"
        else -> "${state.discoveredPeers} peers nearby"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConnectivityBanner(
            headline = headline,
            supportingText = supporting,
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
        HostActionShortcuts(
            enabled = controlsEnabled,
            onStart = { showStartDialog = true },
            onEnd = { showEndDialog = true }
        )
        JoinCodeCard(
            code = state.joinCode,
            expiresIn = state.qrSubtitle,
            peersConnected = state.discoveredPeers,
            qrData = state.qrPayload,
            onCopy = {
                val payload = state.qrPayload.ifBlank { state.joinCode }
                if (payload.isBlank() || payload == "----") return@JoinCodeCard
                clipboard.setText(AnnotatedString(payload))
                Toast.makeText(context, copyConfirmation, Toast.LENGTH_SHORT).show()
            }
        )
        Text(
            text = scanInstructions,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            onClick = { onToggleLeaderboard(!state.hideLeaderboard) },
            enabled = controlsEnabled
        )
        SecondaryButton(
            text = if (state.lockAfterFirst) "Unlock after Q1" else "Lock joins after Q1",
            onClick = { onToggleLock(!state.lockAfterFirst) },
            enabled = controlsEnabled
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val buttonWidth = (maxWidth - 12.dp) / 2
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = "Start",
                    onClick = { showStartDialog = true },
                    modifier = Modifier.width(buttonWidth),
                    enabled = controlsEnabled
                )
                SecondaryButton(
                    text = "End",
                    onClick = { showEndDialog = true },
                    modifier = Modifier.width(buttonWidth),
                    enabled = controlsEnabled
                )
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

@Composable
private fun HostActionShortcuts(
    enabled: Boolean,
    onStart: () -> Unit,
    onEnd: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Host actions",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onStart, enabled = enabled) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(text = "Start")
                }
                TextButton(onClick = onEnd, enabled = enabled) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(text = "End")
                }
            }
            Text(
                text = "Quick access if the footer buttons are out of view.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@QuizPreviews
@Composable
private fun LaunchLobbyPreview() {
    QuizMasterTheme {
        LaunchLobbyScreen(
            state = LaunchLobbyUiState(
                joinCode = "R7FT",
                qrSubtitle = "09:12",
                qrPayload = "ws://192.168.0.10:48765/ws?token=demo",
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
