package com.classroom.quizmaster.ui.feature.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModuleBuilderScreen(
    viewModel: ModuleBuilderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Module") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.topic,
                onValueChange = viewModel::onTopicChanged,
                label = { Text("Paksa / Topic") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.objectives,
                onValueChange = viewModel::onObjectivesChanged,
                label = { Text("Learning Objectives (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.slides,
                onValueChange = viewModel::onSlidesChanged,
                label = { Text("Lesson Slides (one per line)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            )
            OutlinedTextField(
                value = state.timePerItem,
                onValueChange = viewModel::onTimePerItemChanged,
                label = { Text("Time per item (seconds)") },
                modifier = Modifier.fillMaxWidth()
            )
            if (state.errors.isNotEmpty()) {
                state.errors.forEach { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            state.message?.let { message -> Text(message, color = MaterialTheme.colorScheme.primary) }
            Button(onClick = { viewModel.save(onBack) }, modifier = Modifier.fillMaxWidth()) {
                Text("I-save ang Module")
            }
        }
    }
}
