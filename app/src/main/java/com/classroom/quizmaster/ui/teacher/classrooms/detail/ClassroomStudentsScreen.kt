package com.classroom.quizmaster.ui.teacher.classrooms.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ClassroomStudentsRoute(
    onBack: () -> Unit,
    viewModel: ClassroomStudentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { message ->
            if (message.isNotBlank()) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    ClassroomStudentsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIdentifierChanged = viewModel::updateIdentifier,
        onAddStudent = viewModel::addStudent,
        onApprove = viewModel::approveRequest,
        onDecline = viewModel::declineRequest,
        onRemove = viewModel::removeStudent,
        onBack = onBack
    )
}

@Composable
fun ClassroomStudentsScreen(
    state: ClassroomStudentsUiState,
    snackbarHostState: SnackbarHostState,
    onIdentifierChanged: (String) -> Unit,
    onAddStudent: () -> Unit,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit
) {
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(text = state.classroomName.ifBlank { "Students" }, style = MaterialTheme.typography.titleLarge)
                    state.joinCode.takeIf { it.isNotBlank() }?.let { code ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Code: $code", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy join code")
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AddStudentCard(
                identifier = state.identifier,
                isAdding = state.isAdding,
                errorMessage = state.addError,
                onIdentifierChanged = onIdentifierChanged,
                onAddStudent = onAddStudent,
                isDisabled = state.isArchived
            )

            if (state.pendingRequests.isNotEmpty()) {
                Text(text = "Pending requests", style = MaterialTheme.typography.titleLarge)
                state.pendingRequests.forEach { request ->
                    JoinRequestRow(
                        request = request,
                        isProcessing = state.processingRequestId == request.id,
                        onApprove = { onApprove(request.id) },
                        onDecline = { onDecline(request.id) }
                    )
                }
            }

            Text(text = "Students in this class", style = MaterialTheme.typography.titleLarge)
            if (state.students.isEmpty()) {
                EmptyState(
                    title = "No students yet",
                    message = "Add students manually or approve join requests."
                )
            } else {
                state.students.forEach { student ->
                    StudentRow(
                        student = student,
                        onRemove = { pendingRemoval = student.id },
                        isRemoving = state.removingStudentId == student.id,
                        enabled = !state.isArchived
                    )
                }
            }
        }
    }

    pendingRemoval?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(target)
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
            title = { Text("Remove student?") },
            text = { Text("Remove this student from the classroom? They will lose access to its content and assignments.") }
        )
    }
}

@Composable
fun ClassroomStudentsTabContent(
    state: ClassroomStudentsUiState,
    onIdentifierChanged: (String) -> Unit,
    onAddStudent: () -> Unit,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AddStudentCard(
            identifier = state.identifier,
            isAdding = state.isAdding,
            errorMessage = state.addError,
            onIdentifierChanged = onIdentifierChanged,
            onAddStudent = onAddStudent,
            isDisabled = state.isArchived
        )
        if (state.pendingRequests.isNotEmpty()) {
            Text(text = "Pending requests", style = MaterialTheme.typography.titleLarge)
            state.pendingRequests.forEach { request ->
                JoinRequestRow(
                    request = request,
                    isProcessing = state.processingRequestId == request.id,
                    onApprove = { onApprove(request.id) },
                    onDecline = { onDecline(request.id) }
                )
            }
        }
        Text(text = "Students in this class", style = MaterialTheme.typography.titleLarge)
        if (state.students.isEmpty()) {
            EmptyState(
                title = "No students yet",
                message = "Add students manually or approve join requests."
            )
        } else {
            state.students.forEach { student ->
                StudentRow(
                    student = student,
                    onRemove = { pendingRemoval = student.id },
                    isRemoving = state.removingStudentId == student.id,
                    enabled = !state.isArchived
                )
            }
        }
    }
    pendingRemoval?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(target)
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
            title = { Text("Remove student?") },
            text = { Text("Remove this student from the classroom? They will lose access to its content and assignments.") }
        )
    }
}

@Composable
fun AddStudentCard(
    identifier: String,
    isAdding: Boolean,
    errorMessage: String?,
    onIdentifierChanged: (String) -> Unit,
    onAddStudent: () -> Unit,
    isDisabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Add student by email or username", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = identifier,
                onValueChange = onIdentifierChanged,
                label = { Text("Email or username") },
                placeholder = { Text("student@email.com or username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isAdding && !isDisabled,
                isError = !errorMessage.isNullOrBlank()
            )
            Text(
                text = "Student must already have an account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton(
                text = if (isAdding) "Adding..." else "Add",
                onClick = onAddStudent,
                enabled = identifier.isNotBlank() && !isAdding && !isDisabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun JoinRequestRow(
    request: JoinRequestUi,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(request.studentName, style = MaterialTheme.typography.titleMedium)
            if (request.contact.isNotBlank()) {
                Text(request.contact, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Requested ${request.requestedAgo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = if (isProcessing) "Working..." else "Approve",
                    onClick = onApprove,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDecline, enabled = !isProcessing, modifier = Modifier.weight(1f)) {
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
fun StudentRow(
    student: StudentRowUi,
    onRemove: () -> Unit,
    isRemoving: Boolean,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(student.name, style = MaterialTheme.typography.titleMedium)
            if (student.contact.isNotBlank()) {
                Text(student.contact, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Joined ${student.joined}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onRemove,
                    enabled = enabled && !isRemoving
                ) {
                    Text(if (isRemoving) "Removing..." else "Remove")
                }
            }
        }
    }
}
