package com.classroom.quizmaster.ui.teacher.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
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
        item {
            ImprovementSummary(state)
        }
        if (state.studentImprovement.isNotEmpty()) {
            item {
                Text(text = "Pre/Post improvement", style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(
                state.studentImprovement,
                key = { index, row -> "improve-$index-${row.name}" }
            ) { _, row ->
                StudentImprovementRow(row)
            }
        }
        if (state.studentProgress.isNotEmpty()) {
            item {
                Text(text = "Student progress", style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(
                state.studentProgress,
                key = { index, row -> "student-$index-${row.name}" }
            ) { _, row ->
                StudentProgressRow(row)
            }
        }
        if (state.questionRows.isEmpty()) {
            item {
                EmptyState(
                    title = "No report data",
                    message = "Run a quiz or assignment to generate item analysis."
                )
            }
        } else {
            itemsIndexed(state.questionRows, key = { index, row -> "${index}-${row.question}" }) { _, row ->
                QuestionRow(row)
            }
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
        if (state.topTopics.isEmpty()) {
            Text(
                text = "No topic progress yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.topTopics.forEach { topic ->
                Text("- $topic")
            }
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
    val updatedLabel = state.lastUpdated.takeIf { it.isNotBlank() } ?: "Not updated"
    Text(text = "Updated $updatedLabel", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ImprovementSummary(state: ReportsUiState) {
    if (state.classPreAverage == 0 && state.classPostAverage == 0 && state.classDelta == 0) return
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileWidth = (maxWidth - 24.dp) / 3
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                title = "Class pre-test",
                value = "${state.classPreAverage}%",
                modifier = Modifier.width(tileWidth)
            )
            SummaryTile(
                title = "Class post-test",
                value = "${state.classPostAverage}%",
                modifier = Modifier.width(tileWidth)
            )
            SummaryTile(
                title = "Gain",
                value = formatDelta(state.classDelta),
                modifier = Modifier.width(tileWidth)
            )
        }
    }
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

@Composable
private fun StudentProgressRow(row: StudentProgressUi) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(row.name, style = MaterialTheme.typography.titleMedium)
            Text("Score ${row.score}% • Completed ${row.completed}/${row.total}")
        }
    }
}

@Composable
private fun StudentImprovementRow(row: StudentImprovementUi) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(row.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "Pre ${row.preAvg}% • Post ${row.postAvg}% • Gain ${formatDelta(row.delta)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Attempts pre ${row.preAttempts}, post ${row.postAttempts}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Float.formatPercent(): String = "${(this * 100).toInt()}%"
private fun formatDelta(delta: Int): String = if (delta >= 0) "+${delta}%" else "${delta}%"

@QuizPreviews
@Composable
private fun ReportsPreview() {
    QuizMasterTheme {
        ReportsScreen(
            state = ReportsUiState(
                average = 82,
                median = 78,
                classPreAverage = 60,
                classPostAverage = 78,
                classDelta = 18,
                topTopics = listOf("Fractions", "Ecosystems", "Grammar"),
                questionRows = listOf(
                    ReportRowUi("Q1 Fractions", 0.65f, "B", 0.24f),
                    ReportRowUi("Q2 Planets", 0.85f, "C", 0.12f)
                ),
                lastUpdated = "2m ago",
                studentProgress = listOf(
                    StudentProgressUi("Alex Rivers", completed = 8, total = 10, score = 88),
                    StudentProgressUi("Jamie Lee", completed = 6, total = 10, score = 72)
                ),
                studentImprovement = listOf(
                    StudentImprovementUi("Alex Rivers", preAvg = 60, postAvg = 88, delta = 28, preAttempts = 1, postAttempts = 1),
                    StudentImprovementUi("Jamie Lee", preAvg = 55, postAvg = 70, delta = 15, preAttempts = 1, postAttempts = 1)
                )
            )
        )
    }
}
