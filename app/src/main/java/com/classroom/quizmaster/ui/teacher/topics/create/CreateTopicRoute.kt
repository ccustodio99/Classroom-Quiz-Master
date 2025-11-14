package com.classroom.quizmaster.ui.teacher.topics.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton

@Composable
fun CreateTopicRoute(
    onDone: () -> Unit,
    viewModel: CreateTopicViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.success) {
        if (state.success) onDone()
    }
    CreateTopicScreen(
        name = state.name,
        description = state.description,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        onNameChanged = viewModel::updateName,
        onDescriptionChanged = viewModel::updateDescription,
        onSave = { viewModel.save(onDone) },
        onBack = onDone
    )
}

@Composable
fun CreateTopicScreen(
    name: String,
    description: String,
    isSaving: Boolean,
    errorMessage: String?,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val canSave = name.isNotBlank() && !isSaving
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create topic",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Topics sit inside a classroom and hold related quizzes and assignments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Topic name") },
            singleLine = true
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
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
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Save topic",
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
