package com.classroom.quizmaster.ui.teacher.assignments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun AssignmentsRoute(
    onAssignmentSelected: (String) -> Unit,
    viewModel: AssignmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AssignmentsScreen(
        state = state,
        onAssignmentSelected = onAssignmentSelected,
        onRefresh = viewModel::refresh
    )
}

@Composable
fun AssignmentsScreen(
    state: AssignmentsUiState,
    onAssignmentSelected: (String) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Active assignments", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Refresh",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onRefresh() }
                )
            }
        }
        if (state.pending.isEmpty()) {
            item {
                EmptyState(
                    title = "No active assignments",
                    message = "Create one from Teacher Home to share work with students."
                )
            }
        } else {
            items(state.pending, key = { it.id }) { assignment ->
                AssignmentCard(assignment, onAssignmentSelected)
            }
        }
        item {
            Text(
                text = "Closed & archived",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (state.archived.isEmpty()) {
            item {
                EmptyState(
                    title = "No closed assignments",
                    message = "Assignments move here automatically after the due date or when archived."
                )
            }
        } else {
            items(state.archived, key = { it.id }) { assignment ->
                AssignmentCard(assignment, onAssignmentSelected)
            }
        }
    }
}

@Composable
private fun AssignmentCard(assignment: AssignmentCardUi, onAssignmentSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAssignmentSelected(assignment.id) },
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(assignment.title, style = MaterialTheme.typography.titleMedium)
            Text(assignment.dueIn, style = MaterialTheme.typography.bodyMedium)
            val attemptsLabel = assignment.attemptsAllowed.takeIf { it > 0 }?.let {
                "up to $it attempts"
            }
            Text(
                text = buildString {
                    append("${assignment.submissions} submissions")
                    if (attemptsLabel != null) {
                        append(" | $attemptsLabel")
                    }
                    append(" | ${assignment.statusLabel}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@QuizPreviews
@Composable
private fun AssignmentsPreview() {
    QuizMasterTheme {
        AssignmentsScreen(
            state = AssignmentsUiState(
                pending = listOf(
                    AssignmentCardUi("1", "Homework 1", "2d", 18, 24, "Open")
                )
            ),
            onAssignmentSelected = {},
            onRefresh = {}
        )
    }
}
