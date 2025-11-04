package com.example.lms.feature.live.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LiveRoute(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    LiveScreen(
        modifier = modifier,
        state = viewModel.uiState,
        onExit = onExit,
    )
}

@Composable
fun LiveScreen(
    state: LiveUiState,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var typedResponse by remember { mutableStateOf("") }
    var sliderValue by remember { mutableFloatStateOf(state.questions[state.currentQuestionIndex].sliderRange?.first?.toFloat() ?: 0f) }
    val question = state.questions[state.currentQuestionIndex]
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Live session", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Mode: ${state.mode.name} • Code ${state.sessionCode}", style = MaterialTheme.typography.bodyMedium)
        Text(state.connectionStatus, style = MaterialTheme.typography.labelMedium)
        Button(onClick = onExit) { Text(if (state.mode == LiveMode.HOST) "End session" else "Leave session") }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(question.title, style = MaterialTheme.typography.titleMedium)
                Text(question.prompt, style = MaterialTheme.typography.bodyLarge)
                when (question.type) {
                    "MCQ", "TF", "Puzzle" -> {
                        question.options.forEach { option ->
                            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(option) }
                        }
                    }
                    "Slider" -> {
                        question.sliderRange?.let { range ->
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                valueRange = range.first.toFloat()..range.last.toFloat(),
                            )
                            Text("Selected: ${sliderValue.toInt()}")
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = typedResponse,
                            onValueChange = { typedResponse = it },
                            label = { Text("Type answer") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        Text("Leaderboard", style = MaterialTheme.typography.titleMedium)
        state.leaderboard.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}. ${entry.displayName}", style = MaterialTheme.typography.bodyLarge)
                Column(horizontalAlignment = Alignment.End) {
                    Text("${entry.score.toInt()} pts", style = MaterialTheme.typography.bodyMedium)
                    Text("Streak ${entry.streak} • ${entry.latencyMs} ms", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
