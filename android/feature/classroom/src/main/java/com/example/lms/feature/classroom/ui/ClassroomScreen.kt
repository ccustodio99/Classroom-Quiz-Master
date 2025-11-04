package com.example.lms.feature.classroom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ClassroomRoute(
    modifier: Modifier = Modifier,
    onOpenLive: () -> Unit,
    onViewGrades: () -> Unit,
    viewModel: ClassroomViewModel = hiltViewModel(),
) {
    ClassroomScreen(
        modifier = modifier,
        state = viewModel.uiState,
        onOpenLive = onOpenLive,
        onViewGrades = onViewGrades,
    )
}

@Composable
fun ClassroomScreen(
    state: ClassroomUiState,
    onOpenLive: () -> Unit,
    onViewGrades: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Stream", "Classwork", "People", "Grades")
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${state.className} • ${state.section}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Join code: ${state.joinCode}", style = MaterialTheme.typography.bodyMedium)
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> StreamSection(state)
            1 -> ClassworkSection(state, onOpenLive)
            2 -> PeopleSection(state)
            else -> GradesSection(state, onViewGrades)
        }
    }
}

@Composable
private fun StreamSection(state: ClassroomUiState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.stream) { post ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(post.author, style = MaterialTheme.typography.titleMedium)
                    Text(post.message, style = MaterialTheme.typography.bodyMedium)
                    Text(post.timestamp, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ClassworkSection(state: ClassroomUiState, onOpenLive: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.classwork) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.type, style = MaterialTheme.typography.labelMedium)
                    Text(item.due, style = MaterialTheme.typography.bodySmall)
                    if (item.type.equals("Live", ignoreCase = true)) {
                        Button(onClick = onOpenLive) { Text("Launch live challenge") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleSection(state: ClassroomUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Teachers", style = MaterialTheme.typography.titleMedium)
        state.teachers.forEach { teacher -> Text(teacher, style = MaterialTheme.typography.bodyMedium) }
        Divider()
        Text("Learners", style = MaterialTheme.typography.titleMedium)
        state.learners.forEach { learner -> Text(learner, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun GradesSection(state: ClassroomUiState, onViewGrades: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        state.grades.forEach { grade ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text(grade.category, style = MaterialTheme.typography.titleMedium); Text("Weight ${grade.weight}", style = MaterialTheme.typography.bodySmall) }
                Text(grade.score, style = MaterialTheme.typography.titleLarge)
            }
            Divider()
        }
        Button(onClick = onViewGrades) { Text("Open gradebook") }
    }
}
