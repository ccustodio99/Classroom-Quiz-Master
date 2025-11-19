package com.classroom.quizmaster.ui.teacher.topics.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton

@Composable
fun EditTopicRoute(
    onDone: () -> Unit,
    onArchived: () -> Unit,
    viewModel: EditTopicViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.success) {
        if (state.success) onDone()
    }
    LaunchedEffect(state.archived) {
        if (state.archived) onArchived()
    }
    EditTopicScreen(
        name = state.name,
        description = state.description,
        isLoading = state.isLoading,
        isSaving = state.isSaving,
        isArchiving = state.isArchiving,
        errorMessage = state.errorMessage,
        onNameChanged = viewModel::updateName,
        onDescriptionChanged = viewModel::updateDescription,
        onSave = viewModel::save,
        onArchive = viewModel::archive,
        onBack = onDone
    )
}

@Composable
fun EditTopicScreen(
    name: String,
    description: String,
    isLoading: Boolean,
    isSaving: Boolean,
    isArchiving: Boolean,
    errorMessage: String?,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
    onBack: () -> Unit
) {
    var confirmArchive by remember { mutableStateOf(false) }
    val canEdit = !isSaving && !isArchiving && !isLoading
    val canSave = name.isNotBlank() && canEdit
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Edit topic", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Update the topic name or description. You can archive it if it's no longer needed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = canEdit,
            label = { Text("Topic name") },
            singleLine = true
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            enabled = canEdit,
            label = { Text("Description (optional)") }
        )
        errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = onBack,
                enabled = canEdit,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Save changes",
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.weight(1f)
            )
        }
        SecondaryButton(
            text = "Archive topic",
            onClick = { confirmArchive = true },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archive topic?") },
            text = {
                Text("Students won't see quizzes from archived topics, but you can restore them later by recreating the topic.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmArchive = false
                        onArchive()
                    },
                    enabled = !isArchiving
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
