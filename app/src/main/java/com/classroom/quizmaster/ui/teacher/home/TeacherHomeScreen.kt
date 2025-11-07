package com.classroom.quizmaster.ui.teacher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.ui.components.ConnectivityBanner

@Composable
fun TeacherHomeRoute(
    onCreateQuiz: () -> Unit,
    onLaunchLive: (Quiz) -> Unit,
    onReports: () -> Unit,
    onLogout: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }
    TeacherHomeScreen(
        state = state,
        onCreateQuiz = onCreateQuiz,
        onLaunchLive = onLaunchLive,
        onReports = onReports,
        onLogout = {
            viewModel.logout()
            onLogout()
        }
    )
}

@Composable
fun TeacherHomeScreen(
    state: TeacherHomeUiState,
    onCreateQuiz: () -> Unit,
    onLaunchLive: (Quiz) -> Unit,
    onReports: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onLogout) {
            Text("Logout", color = MaterialTheme.colorScheme.error)
        }
        ConnectivityBanner(
            lanConnected = state.quizzes.isNotEmpty(),
            cloudSyncing = state.isSyncing
        )
        if (state.pendingOps > 0) {
            Text(
                text = "Pending sync items: ${state.pendingOps}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "Classroom Quiz Master",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onCreateQuiz) {
            Text("Create Quiz")
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.quizzes, key = { it.id }) { quiz ->
                QuizCard(quiz = quiz, onLaunch = { onLaunchLive(quiz) })
            }
        }
        Button(onClick = onReports) {
            Text("Reports")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuizCard(quiz: Quiz, onLaunch: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = quiz.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${quiz.questions.size} questions",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onLaunch) {
                Text("Launch Live")
            }
        }
    }
}
