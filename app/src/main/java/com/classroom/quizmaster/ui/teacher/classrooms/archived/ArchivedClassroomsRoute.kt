package com.classroom.quizmaster.ui.teacher.classrooms.archived

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.classroom.quizmaster.ui.components.SecondaryButton

@Composable
fun ArchivedClassroomsRoute(
    onBack: () -> Unit,
    viewModel: ArchivedClassroomsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ArchivedClassroomsScreen(
        state = state,
        onBack = onBack
    )
}

@Composable
fun ArchivedClassroomsScreen(
    state: ArchivedClassroomsUiState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SecondaryButton(text = "Back", onClick = onBack)
        Text(
            text = "Archived classrooms",
            style = MaterialTheme.typography.headlineSmall
        )
        if (state.classrooms.isEmpty()) {
            EmptyState(
                title = "Nothing archived",
                message = "Archived classrooms will appear here for reference."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.classrooms, key = { it.id }) { classroom ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(classroom.name, style = MaterialTheme.typography.titleMedium)
                            classroom.meta?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (classroom.topics.isEmpty()) {
                                Text(
                                    text = "No archived topics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                classroom.topics.forEach { topic ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = topic.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        topic.description.takeIf { it.isNotBlank() }?.let { desc ->
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

