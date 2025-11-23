package com.classroom.quizmaster.ui.student.classrooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.SimpleTopBar
import com.classroom.quizmaster.ui.model.AssignmentCardUi

@Composable
fun StudentClassroomDetailRoute(
    onBack: () -> Unit,
    viewModel: StudentClassroomDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentClassroomDetailScreen(state = state, onBack = onBack)
}

@Composable
fun StudentClassroomDetailScreen(
    state: StudentClassroomDetailUiState,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTopBar(
            title = state.classroomName.ifBlank { "Classroom" },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
        )
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading classroom…")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(state.classroomName, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${state.subject} • ${state.grade}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.teacherName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (state.joinCode.isNotBlank()) {
                                Text(
                                    text = "Code: ${state.joinCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item {
                    Text("Topics & quizzes", style = MaterialTheme.typography.titleMedium)
                }
                if (state.topics.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No topics yet",
                            message = "When your teacher publishes topics they'll appear here."
                        )
                    }
                } else {
                    items(state.topics) { topic ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(topic.name, style = MaterialTheme.typography.titleMedium)
                                if (topic.description.isNotBlank()) {
                                    Text(
                                        topic.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Text("Assignments", style = MaterialTheme.typography.titleMedium)
                }
                if (state.assignments.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No assignments",
                            message = "Assignments for this class will be listed here."
                        )
                    }
                } else {
                    items(state.assignments, key = { it.id }) { assignment ->
                        AssignmentCardCompact(assignment)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentCardCompact(assignment: AssignmentCardUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(assignment.title, style = MaterialTheme.typography.titleMedium)
            Text(assignment.dueIn, style = MaterialTheme.typography.bodySmall)
            Text(
                text = assignment.statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
