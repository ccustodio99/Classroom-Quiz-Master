package com.classroom.quizmaster.ui.student.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.SimpleTopBar

@Composable
fun TeacherSearchRoute(
    onBack: () -> Unit,
    viewModel: TeacherSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TeacherSearchScreen(
        uiState = uiState,
        onBack = onBack,
        onQueryChanged = viewModel::onSearchQueryChanged,
        onTeacherSelected = viewModel::onTeacherSelected,
        onJoinClassroom = viewModel::joinClassroom
    )
}

@Composable
fun TeacherSearchScreen(
    uiState: TeacherSearchUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onTeacherSelected: (Teacher) -> Unit,
    onJoinClassroom: (String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "Search for a Teacher",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onQueryChanged(it)
                },
                label = { Text("Teacher's Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            uiState.joinMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            uiState.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.selectedTeacher != null) {
                if (uiState.classrooms.isEmpty()) {
                    EmptyState(title = "No Classrooms", message = "This teacher has no active classrooms.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.classrooms) { classroom ->
                            ClassroomCard(
                                classroom = classroom,
                                isJoining = uiState.isJoining,
                                onJoin = {
                                    onJoinClassroom(classroom.id, classroom.teacherId)
                                }
                            )
                        }
                    }
                }
            } else {
                if (uiState.teachers.isEmpty()) {
                    EmptyState(title = "No Teachers Found", message = "No teachers match your search.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.teachers) { teacher ->
                            TeacherCard(teacher = teacher, onClick = { onTeacherSelected(teacher) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherCard(teacher: Teacher, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(teacher.displayName, style = MaterialTheme.typography.titleMedium)
            Text(teacher.email, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ClassroomCard(classroom: Classroom, isJoining: Boolean, onJoin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(classroom.name, style = MaterialTheme.typography.titleMedium)
                    Text(classroom.subject, style = MaterialTheme.typography.bodyMedium)
                }
            Button(onClick = onJoin, enabled = !isJoining) {
                Text("Join")
            }
        }
    }
}
