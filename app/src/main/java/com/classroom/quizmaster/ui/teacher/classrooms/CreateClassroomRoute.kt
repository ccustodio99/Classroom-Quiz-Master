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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateClassroomRoute(
    onDone: () -> Unit,
    viewModel: CreateClassroomViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.success) {
        if (state.success) onDone()
    }
    CreateClassroomScreen(
        name = state.name,
        grade = state.grade,
        subject = state.subject,
        joinCode = state.joinCode,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        onNameChanged = viewModel::updateName,
        onGradeChanged = viewModel::updateGrade,
        onSubjectChanged = viewModel::updateSubject,
        onSave = { viewModel.save(onDone) },
        onBack = onDone
    )
}

@Composable
fun CreateClassroomScreen(
    name: String,
    grade: String,
    subject: String,
    joinCode: String,
    isSaving: Boolean,
    errorMessage: String?,
    onNameChanged: (String) -> Unit,
    onGradeChanged: (String) -> Unit,
    onSubjectChanged: (String) -> Unit,
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
            text = "Set up your first classroom",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Classrooms group topics, quizzes, and assignments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Classroom name") },
            singleLine = true
        )

        OutlinedTextField(
            value = grade,
            onValueChange = onGradeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Grade (optional)") },
            singleLine = true
        )

        OutlinedTextField(
            value = subject,
            onValueChange = onSubjectChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subject (optional)") },
            singleLine = true
        )

        OutlinedTextField(
            value = joinCode,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Join code") },
            readOnly = true
        )

        errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(8.dp))

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
                text = "Save classroom",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = canSave
            )
        }
    }
}
