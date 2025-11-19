package com.classroom.quizmaster.ui.student.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classroom.quizmaster.data.lan.LanServiceDescriptor

@Composable
fun JoinLanRoute(
    onJoined: () -> Unit,
    viewModel: StudentJoinViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.discoverLanHosts() }
    JoinLanScreen(
        state = state,
        onDiscover = viewModel::discoverLanHosts,
        onRetry = viewModel::retryDiscovery,
        onNicknameChange = viewModel::updateNickname,
        onManualUriChange = viewModel::updateManualUri,
        onManualJoin = { viewModel.joinFromUri(onJoined) },
        onJoin = { service -> viewModel.join(service, onJoined) }
    )
}

@Composable
fun JoinLanScreen(
    state: StudentJoinUiState,
    onDiscover: () -> Unit,
    onRetry: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onManualUriChange: (String) -> Unit,
    onManualJoin: () -> Unit,
    onJoin: (service: LanServiceDescriptor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Join a nearby host",
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.nickname,
            onValueChange = onNicknameChange,
            label = { Text("Nickname") },
            isError = state.nicknameError != null,
            supportingText = {
                state.nicknameError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
        Button(onClick = onDiscover, enabled = !state.isDiscovering) {
            Text("Discover hosts")
        }
        if (state.isDiscovering) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text("Scanning LAN for quiz hosts...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.timedOut) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("No hosts responded. Try again?", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                state.services,
                key = { it.token.ifBlank { it.serviceName } }
            ) { service ->
                val displayName = service.teacherName?.takeIf { it.isNotBlank() }
                    ?: service.serviceName.substringBefore('.')
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(displayName, style = MaterialTheme.typography.titleMedium)
                        Text("Join code: ${service.joinCode.ifBlank { "LAN" }}")
                        Text("${service.host}:${service.port}")
                        Button(
                            modifier = Modifier.padding(top = 8.dp),
                            onClick = { onJoin(service) },
                            enabled = !state.isJoining && state.nicknameError == null
                        ) {
                            Text("Join")
                        }
                    }
                }
            }
        }
        Text(
            text = "QR/manual fallback",
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.manualUri,
            onValueChange = onManualUriChange,
            label = { Text("ws://host:port/ws?token=...") }
        )
        Button(
            onClick = onManualJoin,
            enabled = state.manualUri.isNotBlank() && state.nicknameError == null
        ) {
            Text("Join via QR URL")
        }
        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
