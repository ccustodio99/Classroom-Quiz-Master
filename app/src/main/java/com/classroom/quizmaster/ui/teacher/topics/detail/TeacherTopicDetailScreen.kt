package com.classroom.quizmaster.ui.teacher.topics.detail

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
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton

@Composable
fun TeacherTopicDetailRoute(
    onBack: () -> Unit,
    onEditTopic: () -> Unit,
    onCreateMaterial: (String, String) -> Unit,
    onOpenMaterial: (String) -> Unit,
    onCreateQuiz: (String, String) -> Unit,
    onEditQuiz: (String, String, String) -> Unit,
    onLaunchLive: (String, String, String?) -> Unit,
    onViewAssignments: () -> Unit,
    onCreateAssignment: (String, String) -> Unit,
    onEditAssignment: (String) -> Unit,
    viewModel: TeacherTopicDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherTopicDetailScreen(
        state = state,
        onBack = onBack,
        onEditTopic = onEditTopic,
        onCreateMaterial = { onCreateMaterial(viewModel.classroomId, viewModel.topicId) },
        onOpenMaterial = onOpenMaterial,
        onCreateQuiz = { onCreateQuiz(viewModel.classroomId, viewModel.topicId) },
        onEditQuiz = { quizId -> onEditQuiz(viewModel.classroomId, viewModel.topicId, quizId) },
        onLaunchLive = { quizId -> onLaunchLive(viewModel.classroomId, viewModel.topicId, quizId) },
        onViewAssignments = onViewAssignments,
        onCreateAssignment = { onCreateAssignment(viewModel.classroomId, viewModel.topicId) },
        onEditAssignment = onEditAssignment
    )
}

@Composable
fun TeacherTopicDetailScreen(
    state: TopicDetailUiState,
    onBack: () -> Unit,
    onEditTopic: () -> Unit,
    onCreateMaterial: () -> Unit,
    onOpenMaterial: (String) -> Unit,
    onCreateQuiz: () -> Unit,
    onEditQuiz: (String) -> Unit,
    onLaunchLive: (String) -> Unit,
    onViewAssignments: () -> Unit,
    onCreateAssignment: () -> Unit,
    onEditAssignment: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SecondaryButton(text = "Back", onClick = onBack)
        Text(text = state.topicName, style = MaterialTheme.typography.headlineSmall)
        if (state.topicDescription.isNotBlank()) {
            Text(
                text = state.topicDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = state.classroomName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SecondaryButton(
            text = "Edit topic",
            onClick = onEditTopic,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.errorMessage != null) {
            EmptyState(title = "Topic unavailable", message = state.errorMessage)
            return@Column
        }
        Text(text = "Learning materials", style = MaterialTheme.typography.titleLarge)
        PrimaryButton(
            text = "Add material",
            onClick = onCreateMaterial,
            enabled = !state.isLoading
        )
        if (state.materials.isEmpty()) {
            EmptyState(
                title = "No materials yet",
                message = "Attach notes, files, or links for this topic."
            )
        } else {
            state.materials.forEach { material ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMaterial(material.id) },
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = material.title, style = MaterialTheme.typography.titleMedium)
                        if (material.description.isNotBlank()) {
                            Text(
                                text = material.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val attachmentLabel = if (material.attachmentCount == 1) {
                            "1 attachment"
                        } else {
                            "${material.attachmentCount} attachments"
                        }
                        Text(
                            text = "$attachmentLabel - ${material.updatedRelative}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        PrimaryButton(
            text = "Create quiz",
            onClick = onCreateQuiz,
            enabled = !state.isLoading
        )
        if (state.quizzes.isEmpty()) {
            EmptyState(
                title = "No quizzes yet",
                message = "Build a quiz for this topic to get started."
            )
        } else {
            Text(text = "Quizzes", style = MaterialTheme.typography.titleLarge)
            state.quizzes.forEach { quiz ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditQuiz(quiz.id) },
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = quiz.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${quiz.questionCount} questions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = quiz.updatedAgo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecondaryButton(text = "Launch live", onClick = { onLaunchLive(quiz.id) })
                    }
                }
            }
        }
        Text(text = "Assignments", style = MaterialTheme.typography.titleLarge)
        PrimaryButton(
            text = "Assign quiz",
            onClick = onCreateAssignment,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.assignments.isEmpty()) {
            EmptyState(
                title = "No assignments",
                message = "Assign this topic's quizzes for independent practice."
            )
        } else {
            state.assignments.forEach { assignment ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = assignment.quizTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Quiz ID ${assignment.quizId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Opens ${assignment.openAt} · Closes ${assignment.closeAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecondaryButton(
                            text = "Edit assignment",
                            onClick = { onEditAssignment(assignment.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        SecondaryButton(text = "View assignments", onClick = onViewAssignments)
    }
}
