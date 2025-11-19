package com.classroom.quizmaster.ui.teacher.classrooms.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun TeacherClassroomDetailRoute(
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onCreateTopic: () -> Unit,
    viewModel: TeacherClassroomDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherClassroomDetailScreen(
        state = state,
        onBack = onBack,
        onTopicSelected = onTopicSelected,
        onCreateTopic = onCreateTopic
    )
}

@Composable
fun TeacherClassroomDetailScreen(
    state: ClassroomDetailUiState,
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onCreateTopic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SecondaryButton(text = "Back", onClick = onBack)
        Text(text = state.classroomName, style = MaterialTheme.typography.headlineMedium)
        val meta = listOfNotNull(
            state.subject.takeIf { it.isNotBlank() },
            state.grade.takeIf { it.isNotBlank() }?.let { "Grade $it" }
        ).joinToString(separator = " · ")
        if (meta.isNotBlank()) {
            Text(text = meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.errorMessage != null) {
            EmptyState(title = "Classroom unavailable", message = state.errorMessage)
            return@Column
        }
        SecondaryButton(
            text = "Add topic",
            onClick = onCreateTopic
        )
        if (state.topics.isEmpty()) {
            EmptyState(
                title = "No topics yet",
                message = "Create a topic for this classroom to organize quizzes."
            )
        } else {
            Text(text = "Topics", style = MaterialTheme.typography.titleLarge)
            state.topics.forEach { topic ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTopicSelected(topic.id) },
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = topic.name, style = MaterialTheme.typography.titleMedium)
                        if (topic.description.isNotBlank()) {
                            Text(
                                text = topic.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${topic.quizCount} quizzes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
