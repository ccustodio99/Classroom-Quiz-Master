package com.classroom.quizmaster.ui.student.join

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classroom.quizmaster.data.lan.LanServiceDescriptor

@Composable
fun JoinLanRoute(
    onJoined: () -> Unit,
    viewModel: StudentJoinViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    LaunchedEffect(Unit) { viewModel.discoverLanHosts() }
    JoinLanScreen(
        state = state,
        snackbarHostState = snackbarHostState,
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
    snackbarHostState: SnackbarHostState,
    onDiscover: () -> Unit,
    onRetry: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onManualUriChange: (String) -> Unit,
    onManualJoin: () -> Unit,
    onJoin: (service: LanServiceDescriptor) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Join a live quiz") },
                actions = {
                    IconButton(onClick = onDiscover, enabled = !state.isDiscovering) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh host list"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item {
                Text(
                    text = "Find your teacher's session on the same Wi‑Fi or hotspot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.nickname,
                    onValueChange = onNicknameChange,
                    label = { Text("Nickname") },
                    placeholder = { Text("Student") },
                    isError = state.nicknameError != null,
                    supportingText = {
                        Text(
                            text = state.nicknameError
                                ?: "Nicknames are visible to your teacher and classmates.",
                            color = if (state.nicknameError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    singleLine = true
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    ListItem(
                        leadingContent = {
                            if (state.isDiscovering) {
                                CircularProgressIndicator(strokeWidth = 3.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lan,
                                    contentDescription = null
                                )
                            }
                        },
                        headlineContent = {
                            Text(if (state.isDiscovering) "Scanning for hosts..." else "Nearby hosts")
                        },
                        supportingContent = {
                            Crossfade(targetState = state.timedOut, animationSpec = tween(250)) { timedOut ->
                                if (timedOut) {
                                    Text(
                                        text = "No hosts responded. Check Wi‑Fi and retry.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text("Hosts appear automatically when found.")
                                }
                            }
                        },
                        trailingContent = {
                            Button(
                                onClick = onDiscover,
                                enabled = !state.isDiscovering
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (state.isDiscovering) "Scanning" else "Discover")
                            }
                        }
                    )
                    if (state.timedOut) {
                        TextButton(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            onClick = onRetry
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry discovery")
                        }
                    }
                }
            }
            if (state.services.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            leadingContent = {
                                Icon(imageVector = Icons.Default.Devices, contentDescription = null)
                            },
                            headlineContent = { Text("Waiting for hosts") },
                            supportingContent = {
                                Text(
                                    "Ask your teacher to start the session. Stay on the same network to join quickly.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            } else {
                items(
                    state.services,
                    key = { it.token.ifBlank { it.serviceName } }
                ) { service ->
                    val displayName = service.teacherName?.takeIf { it.isNotBlank() }
                        ?: service.serviceName.substringBefore('.')
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Join code • ${service.joinCode.ifBlank { "LAN" }}")
                                    Text("${service.host}:${service.port}")
                                }
                            },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        focusManager.clearFocus(force = true)
                                        onJoin(service)
                                    },
                                    enabled = !state.isJoining && state.nicknameError == null,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = state.isJoining,
                                        label = "join-progress"
                                    ) { joining ->
                                        if (joining) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.padding(end = 8.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    Text("Join")
                                }
                            }
                        )
                    }
                }
            }
            item {
                Text(
                    text = "QR or manual link",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.manualUri,
                    onValueChange = onManualUriChange,
                    label = { Text("Paste ws://host:port/ws?token=...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onManualJoin()
                        },
                        enabled = state.manualUri.isNotBlank() && state.nicknameError == null
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join via link")
                    }
                    TextButton(onClick = onRetry) {
                        Icon(imageVector = Icons.Default.CloudOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Offline help")
                    }
                }
            }
            if (state.nicknameError != null) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.nicknameError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
