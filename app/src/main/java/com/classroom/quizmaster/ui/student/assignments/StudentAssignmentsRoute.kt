package com.classroom.quizmaster.ui.student.assignments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SimpleTopBar
import com.classroom.quizmaster.ui.model.AssignmentCardUi

@Composable
fun StudentAssignmentsRoute(
    onAssignmentSelected: (String) -> Unit,
    viewModel: StudentAssignmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAssignmentsScreen(
        state = state,
        onFilterChanged = viewModel::selectFilter,
        onRefresh = viewModel::refresh,
        onAssignmentSelected = onAssignmentSelected
    )
}

@Composable
fun StudentAssignmentsScreen(
    state: StudentAssignmentsUiState,
    onFilterChanged: (StudentAssignmentFilter) -> Unit,
    onRefresh: () -> Unit,
    onAssignmentSelected: (String) -> Unit
) {
    Scaffold(
        topBar = { SimpleTopBar(title = "Assignments") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items, key = { it.id }) { assignment ->
                        AssignmentCard(assignment, onClick = { onAssignmentSelected(assignment.id) })
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
            val isSelected = selected == filter
            val colors = if (isSelected) {
                androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
            }
            androidx.compose.material3.OutlinedButton(
                onClick = { onSelected(filter) },
                colors = colors,
                border = if (isSelected) null else androidx.compose.material3.ButtonDefaults.outlinedButtonBorder(enabled = true),
                contentPadding = androidx.compose.material3.ButtonDefaults.ContentPadding
            ) {
                Text(filter.label)
            }
        }
    }
}

@Composable
private fun AssignmentCard(assignment: AssignmentCardUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(assignment.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(assignment.dueIn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${assignment.submissions} submissions • ${assignment.statusLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
