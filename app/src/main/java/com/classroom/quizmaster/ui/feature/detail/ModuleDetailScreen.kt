package com.classroom.quizmaster.ui.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    viewModel: ModuleDetailViewModel,
    onStartDelivery: () -> Unit,
    onViewReports: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val module = state.module
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(module?.topic ?: "Module") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::syncToCloud) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            module?.let {
                Text("Layunin: ${it.objectives.joinToString()}", style = MaterialTheme.typography.bodyLarge)
                Text("Pre-Test Items: ${it.preTest.items.size}")
                Text("Post-Test Items: ${it.postTest.items.size}")
                Button(onClick = viewModel::createLiveSession, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Live Session Code")
                }
                state.liveSessionCode?.let { code ->
                    Text("Session Code: $code", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
                Button(onClick = onStartDelivery, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulan: Pre -> Lesson -> Post")
                }
                Button(onClick = viewModel::assignHomework, modifier = Modifier.fillMaxWidth()) {
                    Text("Assign as Homework")
                }
                Button(onClick = onViewReports, modifier = Modifier.fillMaxWidth()) {
                    Text("View Reports")
                }
            } ?: run {
                Text("Loading module...")
            }
        }
    }
}
