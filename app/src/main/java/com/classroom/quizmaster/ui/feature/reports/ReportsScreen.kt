package com.classroom.quizmaster.ui.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                Text("Loading reports...")
            } else if (state.errorMessage != null) {
                Text("Error: ${state.errorMessage}")
                Button(onClick = viewModel::refresh) { Text("Retry") }
            } else {
                state.report?.let { report ->
                    Text(report.topic, style = MaterialTheme.typography.titleLarge)
                    Text(String.format("Pre Avg: %.1f%%  Post Avg: %.1f%%", report.preAverage, report.postAverage))
                    Text("Objective Mastery")
                    report.objectiveMastery.values.forEach { mastery ->
                        Text("${mastery.objective}: Pre ${"%.1f".format(mastery.pre)}% → Post ${"%.1f".format(mastery.post)}%")
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = true),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(report.attempts) { summary ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(summary.student.displayName, style = MaterialTheme.typography.titleMedium)
                                Text("Pre: ${summary.prePercent?.let { "%.1f".format(it) } ?: "--"}%")
                                Text("Post: ${summary.postPercent?.let { "%.1f".format(it) } ?: "--"}%")
                            }
                        }
                    }
                    Button(onClick = viewModel::exportClassPdf, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Text("Export Class PDF")
                    }
                    Button(onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Text("Export CSV")
                    }
                } ?: Text("No report yet. Complete assessments first.")
            }
        }
    }
}
