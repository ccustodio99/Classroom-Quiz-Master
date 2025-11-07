package com.classroom.quizmaster.ui.student.play

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classroom.quizmaster.ui.components.TimerRing

@Composable
fun StudentPlayRoute(
    viewModel: StudentPlayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    StudentPlayScreen(
        state = state,
        onSubmit = { answers ->
            val session = state.session ?: return@StudentPlayScreen
            viewModel.submitAnswer(
                questionId = "q${session.currentIndex}",
                choices = answers,
                correct = listOf("A"),
                timeTaken = 3_000,
                timeLimit = 30_000,
                nonce = System.currentTimeMillis().toString(),
                reveal = state.reveal
            )
        }
    )
}

@Composable
fun StudentPlayScreen(
    state: StudentPlayUiState,
    onSubmit: (List<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.session?.let { "Question ${it.currentIndex + 1}" } ?: "Waiting for host",
                style = MaterialTheme.typography.headlineSmall
            )
            TimerRing(
                progress = (state.timerSeconds / 30f).coerceIn(0f, 1f),
                modifier = Modifier.semantics { contentDescription = "Time remaining ${state.timerSeconds}s" }
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("A", "B", "C", "D")) { choice ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(choice, modifier = Modifier.padding(12.dp))
                }
            }
        }
        Button(onClick = { onSubmit(listOf("A")) }, enabled = state.session != null) {
            Text("Submit")
        }
    }
}
