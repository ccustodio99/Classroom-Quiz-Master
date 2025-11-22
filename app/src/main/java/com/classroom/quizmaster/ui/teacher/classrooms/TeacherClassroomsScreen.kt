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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.SimpleTopBar
import com.classroom.quizmaster.ui.student.classrooms.ClassroomSummaryUi
import com.classroom.quizmaster.ui.student.classrooms.TeacherClassroomUiState
import com.classroom.quizmaster.ui.student.classrooms.TeacherClassroomsViewModel

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
        if (state.classrooms.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) {
                EmptyState(
                    title = "Nothing here",
                    message = "Start by creating a classroom for your class."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(state.classrooms, key = { it.id }) { classroom ->
                    ClassroomRow(
                        summary = classroom,
                        onClick = { onClassroomSelected(classroom.id) },
                        onEdit = { onEditClassroom(classroom.id) },
                        onArchive = { onArchiveClassroom(classroom.id) }
                    )
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
    onArchive: () -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = summary.teacherName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    DropdownMenuItem(text = { Text("Archive") }, onClick = {
                        menuExpanded.value = false
                        onArchive()
                    })
                }
            }
        }
    }
}
