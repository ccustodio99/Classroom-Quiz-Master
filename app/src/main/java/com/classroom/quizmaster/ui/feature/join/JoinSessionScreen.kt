package com.classroom.quizmaster.ui.feature.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.TopBarAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinSessionScreen(
    viewModel: JoinSessionViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    GenZScaffold(
        title = "Join live session",
        subtitle = "Connect over the classroom LAN and play from your device.",
        onBack = onBack,
        actions = listOfNotNull(
            if (state.connected) {
                TopBarAction(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                    onClick = viewModel::requestRefresh
                )
            } else null
        )
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            JoinFormSection(
                state = state,
                onNicknameChanged = viewModel::onNicknameChanged,
                onSessionCodeChanged = viewModel::onSessionCodeChanged,
                onHostChanged = viewModel::onHostAddressChanged,
                onScan = viewModel::scanLocalNetwork,
                onConnect = viewModel::connectToSession,
                onJoinDiscovered = viewModel::joinDiscoveredSession
            )
            if (state.connected) {
                AnswerPadSection(
                    state = state,
                    onSubmitChoice = viewModel::submitChoice,
                    onTypedAnswerChanged = viewModel::onTypedAnswerChanged,
                    onSubmitTyped = viewModel::submitTypedAnswer,
                    onDisconnect = viewModel::disconnect
                )
            }
        }
    }
}

@Composable
private fun JoinFormSection(
    state: JoinSessionUiState,
    onNicknameChanged: (String) -> Unit,
    onSessionCodeChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onScan: () -> Unit,
    onConnect: (String, String) -> Unit,
    onJoinDiscovered: (DiscoveredSession) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ready to join?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNicknameChanged,
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                )
            )
            OutlinedTextField(
                value = state.sessionCode,
                onValueChange = onSessionCodeChanged,
                label = { Text("Class code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.manualHost,
                onValueChange = onHostChanged,
                label = { Text("Host IP / name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onScan, enabled = state.status !is JoinStatus.Scanning) {
                    Text("Scan LAN")
                }
                OutlinedButton(
                    onClick = {
                        val host = state.manualHost.ifBlank {
                            state.availableSessions.firstOrNull { it.sessionId == state.sessionCode }?.host ?: state.hostAddress ?: ""
                        }
                        if (host.isNotBlank() && state.sessionCode.isNotBlank()) {
                            onConnect(host, state.sessionCode)
                        }
                    }
                ) {
                    Text("Connect")
                }
            }
            StatusMessage(state.status)
            if (state.availableSessions.isNotEmpty()) {
                Text(
                    text = "Sessions on your network",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.availableSessions.forEach { session ->
                        SessionRow(session = session, onJoin = onJoinDiscovered)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(status: JoinStatus) {
    val message = when (status) {
        JoinStatus.Idle -> null
        JoinStatus.Scanning -> "Scanning the local network…"
        JoinStatus.Connecting -> "Connecting to host…"
        JoinStatus.Connected -> "Connected!"
        is JoinStatus.Error -> status.message
    }
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SessionRow(
    session: DiscoveredSession,
    onJoin: (DiscoveredSession) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Code: ${session.sessionId}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Host: ${session.host}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Joined: ${session.participants}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { onJoin(session) }) {
                Text("Join this session")
            }
        }
    }
}

@Composable
private fun AnswerPadSection(
    state: JoinSessionUiState,
    onSubmitChoice: (String) -> Unit,
    onTypedAnswerChanged: (String) -> Unit,
    onSubmitTyped: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = state.activePrompt ?: "Waiting for the teacher to pin a question…",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            state.activeObjective?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            val enabled = !state.activePrompt.isNullOrBlank()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnswerButton(label = "Red", color = Color(0xFFE74C3C), enabled = enabled) { onSubmitChoice("RED") }
                AnswerButton(label = "Blue", color = Color(0xFF3498DB), enabled = enabled) { onSubmitChoice("BLUE") }
                AnswerButton(label = "Yellow", color = Color(0xFFF1C40F), enabled = enabled) { onSubmitChoice("YELLOW") }
                AnswerButton(label = "Green", color = Color(0xFF2ECC71), enabled = enabled) { onSubmitChoice("GREEN") }
            }
            OutlinedTextField(
                value = state.pendingTypedAnswer,
                onValueChange = onTypedAnswerChanged,
                label = { Text("Type your answer") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSubmitTyped(state.pendingTypedAnswer) }, enabled = state.pendingTypedAnswer.isNotBlank()) {
                    Text("Submit text answer")
                }
                OutlinedButton(onClick = onDisconnect) {
                    Text("Leave session")
                }
            }
            state.answerAck?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun AnswerButton(label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        modifier = Modifier.weight(1f),
        enabled = enabled
    ) {
        Text(label)
    }
}
