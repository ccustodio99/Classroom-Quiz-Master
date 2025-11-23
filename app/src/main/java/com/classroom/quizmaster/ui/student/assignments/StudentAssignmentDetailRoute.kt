package com.classroom.quizmaster.ui.student.assignments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.SimpleTopBar

@Composable
fun StudentAssignmentDetailRoute(
    onBack: () -> Unit,
    onStart: (String) -> Unit,
    viewModel: StudentAssignmentDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAssignmentDetailScreen(
        state = state,
        onBack = onBack,
        onStart = { onStart(state.assignmentId) },
        onRecordStart = viewModel::startAssignment
    )
}

@Composable
fun StudentAssignmentDetailScreen(
    state: StudentAssignmentDetailUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onRecordStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SimpleTopBar(
            title = state.title.ifBlank { "Assignment" },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status: ${state.statusLabel}", style = MaterialTheme.typography.bodyMedium)
                Text("Due: ${state.dueLabel.ifBlank { "No due date" }}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Attempts: ${state.attemptsUsed} / ${state.attemptsAllowed}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.message != null) {
                    Text(text = state.message ?: "", color = MaterialTheme.colorScheme.primary)
                }
                if (state.error != null) {
                    Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Button(
            onClick = onStart,
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSubmitting) "Submitting..." else "Start assignment")
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (state.canStart) {
            Button(
                onClick = onRecordStart,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Record attempt")
            }
        }
    }
}
