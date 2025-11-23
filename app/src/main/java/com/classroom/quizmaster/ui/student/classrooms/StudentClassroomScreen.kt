package com.classroom.quizmaster.ui.student.classrooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.SimpleTopBar

@Composable
fun StudentClassroomRoute(
    onJoinClassroom: () -> Unit,
    onOpenClassroom: (String) -> Unit,
    viewModel: StudentClassroomViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentClassroomScreen(
        state = state,
        onJoinClassroom = onJoinClassroom,
        onOpenClassroom = onOpenClassroom
    )
}

@Composable
fun StudentClassroomScreen(
    state: StudentClassroomUiState,
    onJoinClassroom: () -> Unit,
    onOpenClassroom: (String) -> Unit
) {
    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "My Classrooms"
            )
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
                    title = "No classrooms yet",
                    message = "Join a classroom to get started."
                )
                Button(onClick = onJoinClassroom) {
                    Text("Join a classroom")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(state.classrooms, key = { it.id }) { classroom ->
                    StudentClassroomCard(summary = classroom, onClick = { onOpenClassroom(classroom.id) })
                }
            }
        }
    }
}

@Composable
private fun StudentClassroomCard(summary: ClassroomSummaryUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(summary.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "${summary.subject} • ${summary.grade}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary.teacherName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summary.activeAssignments > 0) {
                Text(
                    text = "${summary.activeAssignments} active assignments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
