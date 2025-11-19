package com.classroom.quizmaster.ui.teacher.classrooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.GhostButton
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton

@Composable
fun EditClassroomRoute(
    onDone: () -> Unit,
    onArchived: () -> Unit,
    viewModel: EditClassroomViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onDone()
    }

    LaunchedEffect(state.archiveSuccess) {
        if (state.archiveSuccess) onArchived()
    }

    EditClassroomScreen(
        state = state,
        onBack = onDone,
        onNameChanged = viewModel::updateName,
        onGradeChanged = viewModel::updateGrade,
        onSubjectChanged = viewModel::updateSubject,
        onSave = viewModel::save,
        onArchive = viewModel::archive
    )
}

@Composable
fun EditClassroomScreen(
    state: EditClassroomUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onGradeChanged: (String) -> Unit,
    onSubjectChanged: (String) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit
) {
    val isBusy = state.isSaving || state.isArchiving || state.isLoading
    var confirmArchive by remember { mutableStateOf(false) }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmArchive = false
                        onArchive()
                    },
                    enabled = !state.isArchiving
                ) {
                    Text(
                        text = "Archive classroom",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Archive this classroom?") },
            text = {
                Text(
                    text = "Archiving hides the classroom, its topics, and quizzes from active lists. You can still review them in the archived view."
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Edit classroom",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Update the classroom details or archive it when the term ends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Classroom name") },
            enabled = !isBusy,
            singleLine = true
        )
        OutlinedTextField(
            value = state.grade,
            onValueChange = onGradeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Grade (optional)") },
            enabled = !isBusy,
            singleLine = true
        )
        OutlinedTextField(
            value = state.subject,
            onValueChange = onSubjectChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subject (optional)") },
            enabled = !isBusy,
            singleLine = true
        )
        state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = onBack,
                enabled = !isBusy,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Save changes",
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.weight(1f)
            )
        }
        GhostButton(
            text = if (state.isArchiving) "Archiving..." else "Archive classroom",
            onClick = { confirmArchive = true },
            enabled = !state.isArchiving && !state.isSaving && !state.isLoading,
            contentColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Archived classrooms move out of the main list but remain available for reports.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
