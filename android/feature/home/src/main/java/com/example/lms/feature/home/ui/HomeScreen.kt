package com.example.lms.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    onContinueLearning: () -> Unit,
    onOpenClassroom: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    HomeScreen(
        modifier = modifier,
        state = viewModel.uiState,
        onContinueLearning = onContinueLearning,
        onOpenClassroom = onOpenClassroom,
        onOpenProfile = onOpenProfile,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onContinueLearning: () -> Unit,
    onOpenClassroom: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Welcome back, ${state.learnerName}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Learning streak: ${state.streakDays} days", style = MaterialTheme.typography.titleMedium)
                Text("${state.minutesToGoal} min to hit today's goal", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onContinueLearning) {
                    Text("Continue ${state.currentModule}")
                }
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Today's tasks", style = MaterialTheme.typography.titleMedium)
                state.todayTasks.forEach { task ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(task.type, style = MaterialTheme.typography.labelMedium)
                        Text(task.due, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(onClick = onOpenClassroom) {
                    Text("Open classroom stream")
                }
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Messages", style = MaterialTheme.typography.titleMedium)
                state.messages.forEachIndexed { index, message ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        if (index != state.messages.lastIndex) {
                            Divider()
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onOpenProfile) { Text("Preferences") }
                    Button(onClick = onOpenClassroom) { Text("View announcements") }
                }
            }
        }
    }
}
