package com.classroom.quizmaster.ui.teacher.classrooms.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip

@Composable
fun TeacherClassroomDetailRoute(
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onCreateTopic: () -> Unit,
    onEditClassroom: () -> Unit,
    onCreatePreTest: (String, String) -> Unit,
    onCreatePostTest: (String, String) -> Unit,
    onEditTest: (String, String, String) -> Unit,
    viewModel: TeacherClassroomDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherClassroomDetailScreen(
        state = state,
        onBack = onBack,
        onTopicSelected = onTopicSelected,
        onCreateTopic = onCreateTopic,
        onEditClassroom = onEditClassroom,
        onCreatePreTest = {
            if (state.defaultTopicId.isNotBlank()) {
                onCreatePreTest(viewModel.classroomId, state.defaultTopicId)
            }
        },
        onCreatePostTest = {
            if (state.defaultTopicId.isNotBlank()) {
                onCreatePostTest(viewModel.classroomId, state.defaultTopicId)
            }
        },
        onEditTest = { test -> onEditTest(viewModel.classroomId, test.topicId, test.id) },
        onDeleteTest = { test -> viewModel.deleteTest(test.id) }
    )
}

@Composable
fun TeacherClassroomDetailScreen(
    state: ClassroomDetailUiState,
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onCreateTopic: () -> Unit,
    onEditClassroom: () -> Unit,
    onCreatePreTest: () -> Unit,
    onCreatePostTest: () -> Unit,
    onEditTest: (ClassroomTestUi) -> Unit,
    onDeleteTest: (ClassroomTestUi) -> Unit
) {
    val pendingDelete = remember { mutableStateOf<ClassroomTestUi?>(null) }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(
                text = "Edit classroom",
                onClick = onEditClassroom,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Add topic",
                onClick = onCreateTopic,
                modifier = Modifier.weight(1f)
            )
        }
        Text(text = "Diagnostic tests", style = MaterialTheme.typography.titleLarge)
        if (!state.canCreateTests) {
            EmptyState(
                title = "Create a topic first",
                message = "Diagnostic tests need at least one topic to store questions."
            )
        } else {
            DiagnosticTestCard(
                label = "Pre test",
                test = state.preTest,
                onCreate = onCreatePreTest,
                onEdit = onEditTest,
                onRequestDelete = { pendingDelete.value = it }
            )
            DiagnosticTestCard(
                label = "Post test",
                test = state.postTest,
                onCreate = onCreatePostTest,
                onEdit = onEditTest,
                onRequestDelete = { pendingDelete.value = it }
            )
        }
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
        pendingDelete.value?.let { test ->
            AlertDialog(
                onDismissRequest = { pendingDelete.value = null },
                title = { Text("Delete ${test.title}") },
                text = { Text("This will archive ${test.title}. You can recreate it later.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteTest(test)
                            pendingDelete.value = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete.value = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DiagnosticTestCard(
    label: String,
    test: ClassroomTestUi?,
    onCreate: () -> Unit,
    onEdit: (ClassroomTestUi) -> Unit,
    onRequestDelete: (ClassroomTestUi) -> Unit
) {
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
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            if (test == null) {
                Text(
                    text = "No $label configured yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecondaryButton(text = "Create $label", onClick = onCreate)
            } else {
                Text(text = test.title, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(text = "Topic: ${test.topicName}")
                    TagChip(text = "${test.questionCount} questions")
                    TagChip(text = "Updated ${test.updatedAgo}")
                }
                PrimaryButton(text = "Edit $label", onClick = { onEdit(test) })
                TextButton(onClick = { onRequestDelete(test) }) {
                    Text("Delete $label")
                }
            }
        }
    }
}
