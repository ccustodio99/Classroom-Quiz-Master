package com.classroom.quizmaster.ui.teacher.classrooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.SimpleTopBar
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.student.classrooms.ClassroomSummaryUi

@Composable
fun TeacherClassroomsRoute(
    onBack: () -> Unit,
    onCreateClassroom: () -> Unit,
    onClassroomSelected: (String) -> Unit,
    onEditClassroom: (String) -> Unit,
    viewModel: TeacherClassroomsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherClassroomsScreen(
        state = state,
        onBack = onBack,
        onCreateClassroom = onCreateClassroom,
        onClassroomSelected = onClassroomSelected,
        onEditClassroom = onEditClassroom,
        onArchiveClassroom = { classroomId -> viewModel.archive(classroomId) }
    )
}

@Composable
fun TeacherClassroomsScreen(
    state: TeacherClassroomUiState,
    onBack: () -> Unit,
    onCreateClassroom: () -> Unit,
    onClassroomSelected: (String) -> Unit,
    onEditClassroom: (String) -> Unit,
    onArchiveClassroom: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(TeacherClassroomsTab.Active) }
    val classrooms = when (selectedTab) {
        TeacherClassroomsTab.Active -> state.activeClassrooms
        TeacherClassroomsTab.Archived -> state.archivedClassrooms
    }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "Classrooms",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClassroom) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Create classroom")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                TeacherClassroomsTab.values().forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) }
                    )
                }
            }
            if (classrooms.isEmpty()) {
                EmptyState(
                    title = "Nothing here",
                    message = if (selectedTab == TeacherClassroomsTab.Active) {
                        "Start by creating a classroom for your class."
                    } else {
                        "Archived classrooms will appear here."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(classrooms, key = { it.id }) { classroom ->
                        ClassroomRow(
                            summary = classroom,
                            onClick = { onClassroomSelected(classroom.id) },
                            onEdit = { onEditClassroom(classroom.id) },
                            onArchive = { onArchiveClassroom(classroom.id) },
                            isArchived = selectedTab == TeacherClassroomsTab.Archived
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassroomRow(
    summary: ClassroomSummaryUi,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    isArchived: Boolean
) {
    val menuExpanded = remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(summary.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val meta = listOfNotNull(
                        summary.subject.takeIf { it.isNotBlank() },
                        summary.grade.takeIf { it.isNotBlank() }?.let { "Grade $it" }
                    ).joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagChip(text = "Students: ${summary.studentCount}")
                        TagChip(text = "Code: ${summary.joinCode}")
                    }
                }
                IconButton(onClick = { menuExpanded.value = true }) {
                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "Classroom actions")
                }
                DropdownMenu(expanded = menuExpanded.value, onDismissRequest = { menuExpanded.value = false }) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = {
                        menuExpanded.value = false
                        onClick()
                    })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = {
                        menuExpanded.value = false
                        onEdit()
                    })
                    if (!isArchived) {
                        DropdownMenuItem(text = { Text("Archive") }, onClick = {
                            menuExpanded.value = false
                            onArchive()
                        })
                    }
                }
            }
        }
    }
}

private enum class TeacherClassroomsTab(val label: String) {
    Active("Active"),
    Archived("Archived")
}
