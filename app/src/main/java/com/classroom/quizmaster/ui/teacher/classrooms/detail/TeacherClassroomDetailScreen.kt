package com.classroom.quizmaster.ui.teacher.classrooms.detail

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.R
import com.classroom.quizmaster.config.FeatureToggles
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TeacherClassroomDetailRoute(
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onCreateTopic: () -> Unit,
    onEditClassroom: () -> Unit,
    onCreatePreTest: (String, String) -> Unit,
    onCreatePostTest: (String, String) -> Unit,
    onEditTest: (String, String, String) -> Unit,
    onCreateMaterial: (String) -> Unit,
    onOpenMaterial: (String) -> Unit,
    onAssignments: (String) -> Unit,
    onLaunchLive: (String) -> Unit,
    onReports: (String) -> Unit,
    viewModel: TeacherClassroomDetailViewModel = hiltViewModel(),
    studentsViewModel: ClassroomStudentsViewModel = hiltViewModel()
) {
    val detailState by viewModel.uiState.collectAsStateWithLifecycle()
    val studentsState by studentsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        studentsViewModel.events.collectLatest { message ->
            if (message.isNotBlank()) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    TeacherClassroomDetailScreen(
        detailState = detailState,
        studentsState = studentsState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTopicSelected = onTopicSelected,
        onCreateTopic = onCreateTopic,
        onEditClassroom = onEditClassroom,
        onCreatePreTest = {
            if (detailState.defaultTopicId.isNotBlank()) {
                onCreatePreTest(viewModel.classroomId, detailState.defaultTopicId)
            }
        },
        onCreatePostTest = {
            if (detailState.defaultTopicId.isNotBlank()) {
                onCreatePostTest(viewModel.classroomId, detailState.defaultTopicId)
            }
        },
        onEditTest = { test -> onEditTest(viewModel.classroomId, test.topicId, test.id) },
        onDeleteTest = { test -> viewModel.deleteTest(test.id) },
        onCreateMaterial = { onCreateMaterial(viewModel.classroomId) },
        onOpenMaterial = onOpenMaterial,
        onIdentifierChanged = studentsViewModel::updateIdentifier,
        onAddStudent = studentsViewModel::addStudent,
        onApproveRequest = studentsViewModel::approveRequest,
        onDeclineRequest = studentsViewModel::declineRequest,
        onRemoveStudent = studentsViewModel::removeStudent,
        onAssignments = { onAssignments(viewModel.classroomId) },
        onLaunchLive = { onLaunchLive(viewModel.classroomId) },
        onReports = { onReports(viewModel.classroomId) }
    )
}

@Composable
fun TeacherClassroomDetailScreen(
    detailState: ClassroomDetailUiState,
    studentsState: ClassroomStudentsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onCreateTopic: () -> Unit,
    onEditClassroom: () -> Unit,
    onCreatePreTest: () -> Unit,
    onCreatePostTest: () -> Unit,
    onEditTest: (ClassroomTestUi) -> Unit,
    onDeleteTest: (ClassroomTestUi) -> Unit,
    onCreateMaterial: () -> Unit,
    onOpenMaterial: (String) -> Unit,
    onIdentifierChanged: (String) -> Unit,
    onAddStudent: () -> Unit,
    onApproveRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onRemoveStudent: (String) -> Unit,
    onAssignments: () -> Unit,
    onLaunchLive: () -> Unit,
    onReports: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(ClassroomDetailTab.Students) }
    val pendingDelete = remember { mutableStateOf<ClassroomTestUi?>(null) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.teacher_classroom_join_code_copied)

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = detailState.classroomName.ifBlank { "Classroom" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderSection(
                state = detailState,
                onCopyJoinCode = {
                    detailState.joinCode.takeIf { it.isNotBlank() }?.let { code ->
                        clipboard.setText(AnnotatedString(code))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            )

    val availableTabs = ClassroomDetailTab.values().filter { FeatureToggles.LIVE_ENABLED || it != ClassroomDetailTab.Live }
    if (!FeatureToggles.LIVE_ENABLED && selectedTab == ClassroomDetailTab.Live) {
        selectedTab = availableTabs.firstOrNull() ?: ClassroomDetailTab.Students
    }
    TabRow(selectedTabIndex = availableTabs.indexOf(selectedTab).coerceAtLeast(0)) {
        availableTabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { selectedTab = tab },
                text = { Text(tab.label) }
            )
                }
            }

            when {
                detailState.errorMessage != null -> {
                    EmptyState(
                        title = "Classroom unavailable",
                        message = detailState.errorMessage ?: ""
                    )
                }
                else -> {
                    when (selectedTab) {
                        ClassroomDetailTab.Students -> ClassroomStudentsTabContent(
                            state = studentsState,
                            onIdentifierChanged = onIdentifierChanged,
                            onAddStudent = onAddStudent,
                            onApprove = onApproveRequest,
                            onDecline = onDeclineRequest,
                            onRemove = onRemoveStudent
                        )
                        ClassroomDetailTab.Content -> ContentTab(
                            state = detailState,
                            onEditClassroom = onEditClassroom,
                            onCreateTopic = onCreateTopic,
                            onTopicSelected = onTopicSelected,
                            onCreatePreTest = onCreatePreTest,
                            onCreatePostTest = onCreatePostTest,
                            onEditTest = onEditTest,
                            onRequestDelete = { pendingDelete.value = it }
                        )
                        ClassroomDetailTab.Assignments -> AssignmentTab(onAssignments)
                        ClassroomDetailTab.Live -> if (FeatureToggles.LIVE_ENABLED) LiveTab(onLaunchLive) else {}
                        ClassroomDetailTab.Reports -> ReportsTab(onReports)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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

@Composable
private fun HeaderSection(
    state: ClassroomDetailUiState,
    onCopyJoinCode: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TagChip(text = if (state.isArchived) "Archived" else "Active")
            TagChip(text = "Students: ${state.studentCount}")
            state.subject.takeIf { it.isNotBlank() }?.let { TagChip(text = it) }
            state.grade.takeIf { it.isNotBlank() }?.let { TagChip(text = "Grade $it") }
        }
        state.joinCode.takeIf { it.isNotBlank() }?.let { code ->
            JoinCodeShareCard(
                joinCode = code,
                onCopy = onCopyJoinCode
            )
        }
    }
}

@Composable
private fun ContentTab(
    state: ClassroomDetailUiState,
    onEditClassroom: () -> Unit,
    onCreateTopic: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onCreatePreTest: () -> Unit,
    onCreatePostTest: () -> Unit,
    onEditTest: (ClassroomTestUi) -> Unit,
    onRequestDelete: (ClassroomTestUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                modifier = Modifier.weight(1f),
                enabled = !state.isArchived
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
                onRequestDelete = onRequestDelete
            )
            DiagnosticTestCard(
                label = "Post test",
                test = state.postTest,
                onCreate = onCreatePostTest,
                onEdit = onEditTest,
                onRequestDelete = onRequestDelete
            )
        }
        Text(text = "Topics", style = MaterialTheme.typography.titleLarge)
        if (state.topics.isEmpty()) {
            EmptyState(
                title = "No topics yet",
                message = "Create a topic for this classroom to organize quizzes."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
}

@Composable
private fun AssignmentTab(onAssignments: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Assignments", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Schedule quizzes as homework with due dates, attempts, and scoring rules.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(
                text = "Open assignments",
                onClick = onAssignments,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LiveTab(onLaunchLive: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Live sessions", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Host a LAN-first quiz session. Students join over local Wi-Fi with the session code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(
                text = "Live disabled",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReportsTab(onReports: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Reports", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "View performance summaries and export results for quizzes and assignments.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(
                text = "Open reports",
                onClick = onReports,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun JoinCodeShareCard(
    joinCode: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.teacher_classroom_join_code_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = joinCode, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.teacher_classroom_join_code_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.teacher_classroom_join_code_copy_content_description)
                )
            }
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

private enum class ClassroomDetailTab(val label: String) {
    Students("Students"),
    Content("Content"),
    Assignments("Assignments"),
    Live("Live sessions"),
    Reports("Reports")
}
