package com.classroom.quizmaster.ui.teacher.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.model.ReportRowUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun ReportsRoute(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsScreen(state = state)
}

@Composable
fun ReportsScreen(state: ReportsUiState) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Insights", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            SummaryTiles(state)
        }
        item {
            ExportRow(state)
        }
        items(state.questionRows, key = { it.question }) { row ->
            QuestionRow(row)
        }
    }
}

@Composable
private fun SummaryTiles(state: ReportsUiState) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileWidth = (maxWidth - 12.dp) / 2
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                title = "Average",
                value = "${state.average}%",
                modifier = Modifier.width(tileWidth)
            )
            SummaryTile(
                title = "Median",
                value = "${state.median}%",
                modifier = Modifier.width(tileWidth)
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Top topics", style = MaterialTheme.typography.titleMedium)
        state.topTopics.forEach { topic ->
            Text("- $topic")
        }
    }
}

@Composable
private fun SummaryTile(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun ExportRow(state: ReportsUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        PrimaryButton(text = "Export CSV", onClick = {})
        SecondaryButton(text = "Export PDF", onClick = {})
    }
    Text(text = "Updated ${state.lastUpdated}", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun QuestionRow(row: ReportRowUi) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(row.question, style = MaterialTheme.typography.titleMedium)
            Text("p-value ${row.pValue.formatPercent()} - Distractor ${row.topDistractor} ${row.distractorRate.formatPercent()}")
        }
    }
}

private fun Float.formatPercent(): String = "${(this * 100).toInt()}%"

@QuizPreviews
@Composable
private fun ReportsPreview() {
    QuizMasterTheme {
        ReportsScreen(
            state = ReportsUiState(
                average = 82,
                median = 78,
                topTopics = listOf("Fractions", "Ecosystems", "Grammar"),
                questionRows = listOf(
                    ReportRowUi("Q1 Fractions", 0.65f, "B", 0.24f),
                    ReportRowUi("Q2 Planets", 0.85f, "C", 0.12f)
                ),
                lastUpdated = "2m ago"
            )
        )
    }
}
