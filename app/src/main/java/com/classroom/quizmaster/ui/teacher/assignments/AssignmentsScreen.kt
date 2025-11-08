package com.classroom.quizmaster.ui.teacher.assignments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun AssignmentsRoute(
    viewModel: AssignmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AssignmentsScreen(state)
}

@Composable
fun AssignmentsScreen(state: AssignmentsUiState) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.pending.isEmpty()) {
            item {
                EmptyState(
                    title = "No assignments",
                    message = "Create one from Teacher Home."
                )
            }
        } else {
            items(state.pending, key = { it.id }) { assignment ->
                AssignmentCard(assignment)
            }
        }
    }
}

@Composable
private fun AssignmentCard(assignment: AssignmentCardUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(assignment.title, style = MaterialTheme.typography.titleMedium)
            Text("${assignment.submissions}/${assignment.total} submitted")
            Text("Due ${assignment.dueIn} - ${assignment.statusLabel}")
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
            )
        )
    }
}
