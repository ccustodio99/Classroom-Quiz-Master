package com.classroom.quizmaster.ui.student.assignments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.model.AssignmentCardUi

@Composable
fun StudentAssignmentsRoute(
    viewModel: StudentAssignmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAssignmentsScreen(
        state = state,
        onFilterChanged = viewModel::selectFilter,
        onRefresh = viewModel::refresh
    )
}

@Composable
fun StudentAssignmentsScreen(
    state: StudentAssignmentsUiState,
    onFilterChanged: (StudentAssignmentFilter) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Assignments", style = MaterialTheme.typography.headlineSmall)
        RowFilters(
            selected = state.filter,
            onSelected = onFilterChanged
        )
        val items = when (state.filter) {
            StudentAssignmentFilter.Active -> state.active
            StudentAssignmentFilter.Completed -> state.completed
        }
        if (items.isEmpty()) {
            EmptyState(
                title = if (state.filter == StudentAssignmentFilter.Active) {
                    "No active assignments"
                } else {
                    "No completed assignments yet"
                },
                message = "Assignments assigned to you will appear here."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { assignment ->
                    AssignmentCard(assignment)
                }
            }
        }
        PrimaryButton(
            text = if (state.isRefreshing) "Refreshing..." else "Refresh",
            onClick = onRefresh,
            enabled = !state.isRefreshing,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RowFilters(
    selected: StudentAssignmentFilter,
    onSelected: (StudentAssignmentFilter) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StudentAssignmentFilter.values().forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors()
            )
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(assignment.title, style = MaterialTheme.typography.titleMedium)
            Text(assignment.dueIn, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${assignment.submissions} submissions • ${assignment.statusLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
