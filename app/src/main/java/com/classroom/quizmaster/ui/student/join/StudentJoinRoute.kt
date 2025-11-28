package com.classroom.quizmaster.ui.student.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.config.FeatureToggles
import com.classroom.quizmaster.ui.components.SimpleTopBar
import com.classroom.quizmaster.ui.student.classrooms.JoinClassroomViewModel
import com.classroom.quizmaster.ui.student.classrooms.JoinClassroomUiState
import com.classroom.quizmaster.ui.student.entry.StudentEntryViewModel

@Composable
fun StudentJoinRoute(
    onFindTeacher: () -> Unit,
    onOpenJoinLan: () -> Unit,
    onSessionJoined: () -> Unit,
    onCurrentSession: () -> Unit = {},
    viewModel: JoinClassroomViewModel = hiltViewModel(),
    entryViewModel: StudentEntryViewModel = hiltViewModel(),
    statusViewModel: StudentJoinStatusViewModel = hiltViewModel()
) {
    val joinState by viewModel.uiState.collectAsStateWithLifecycle()
    val entryState by entryViewModel.uiState.collectAsStateWithLifecycle()
    val currentSession by statusViewModel.current.collectAsStateWithLifecycle()

    StudentJoinScreen(
        joinState = joinState,
        sessionCode = entryState.joinCode,
        sessionError = entryState.joinCodeError ?: entryState.errorMessage,
        isJoiningSession = entryState.isJoining,
        onSessionCodeChanged = entryViewModel::updateJoinCode,
        onRequestJoin = { code -> viewModel.joinClassroom(code) { } },
        onFindTeacher = onFindTeacher,
        onOpenJoinLan = onOpenJoinLan,
        onJoinSession = {
            entryViewModel.joinByCode(onJoined = onSessionJoined)
        },
        currentSession = currentSession,
        onOpenCurrentSession = onCurrentSession
    )
}

@Composable
fun StudentJoinScreen(
    joinState: JoinClassroomUiState,
    sessionCode: String,
    sessionError: String?,
    isJoiningSession: Boolean,
    onSessionCodeChanged: (String) -> Unit,
    onRequestJoin: (String) -> Unit,
    onFindTeacher: () -> Unit,
    onOpenJoinLan: () -> Unit,
    onJoinSession: () -> Unit,
    currentSession: CurrentSessionUi?,
    onOpenCurrentSession: () -> Unit
) {
    var classroomCode by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(joinState.success) {
        if (joinState.success) classroomCode = ""
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "Join") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Join a classroom", style = MaterialTheme.typography.titleMedium)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = classroomCode,
                        onValueChange = {
                            classroomCode = it
                        },
                        label = { Text("Classroom code") },
                        placeholder = { Text("Enter classroom code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Ascii
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onRequestJoin(classroomCode) },
                            enabled = classroomCode.isNotBlank() && !joinState.isLoading
                        ) {
                            Icon(imageVector = Icons.Outlined.GroupAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (joinState.isLoading) "Sending..." else "Request to join")
                        }
                        TextButton(onClick = onFindTeacher) {
                            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Find teacher")
                        }
                    }
                    joinState.errorMessage?.let { InlineStatusMessage(message = it, isError = true) }
                    joinState.statusMessage?.let { InlineStatusMessage(message = it, isError = false) }
                }
            }

            if (FeatureToggles.LIVE_ENABLED) {
                Text(text = "Join live quiz", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = sessionCode,
                            onValueChange = onSessionCodeChanged,
                            label = { Text("Session code") },
                            placeholder = { Text("Enter session code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Ascii
                            )
                        )
                        if (!sessionError.isNullOrBlank()) {
                            InlineStatusMessage(message = sessionError, isError = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onJoinSession,
                                enabled = sessionCode.isNotBlank() && !isJoiningSession
                            ) {
                                Text(if (isJoiningSession) "Joining..." else "Join session")
                            }
                            TextButton(onClick = onOpenJoinLan) {
                                Icon(imageVector = Icons.Outlined.Lan, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Find nearby sessions")
                            }
                        }
                    }
                }
                currentSession?.let { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Current session", style = MaterialTheme.typography.titleMedium)
                            Text("Code: ${session.joinCode}", style = MaterialTheme.typography.bodyMedium)
                            Text("Status: ${session.status}", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = onOpenCurrentSession,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Return to session")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineStatusMessage(message: String, isError: Boolean) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Text(
        text = message,
        color = color,
        style = MaterialTheme.typography.bodyMedium
    )
}
