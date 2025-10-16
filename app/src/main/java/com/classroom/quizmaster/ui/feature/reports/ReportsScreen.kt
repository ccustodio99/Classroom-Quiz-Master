package com.classroom.quizmaster.ui.feature.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.ClassReport
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.InfoPill
import com.classroom.quizmaster.ui.components.SectionCard
import com.classroom.quizmaster.ui.components.TopBarAction
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val report = state.report

    GenZScaffold(
        title = "Reports",
        subtitle = report?.topic ?: "Analytics and exports",
        onBack = onBack,
        actions = listOf(
            TopBarAction(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                onClick = viewModel::refresh
            )
        )
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when {
                state.isLoading -> {
                    item { LoadingCard() }
                }
                state.errorMessage != null -> {
                    item { ErrorCard(message = state.errorMessage, onRetry = viewModel::refresh) }
                }
                report != null -> {
                    item { OverviewCard(report) }
                    item { MasteryCard(report) }
                    item { AttemptsCard(report) }
                    item { ExportCard(onExportPdf = viewModel::exportClassPdf, onExportCsv = viewModel::exportCsv) }
                }
                else -> {
                    item { EmptyState() }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    SectionCard(
        title = "Loading reports",
        subtitle = "Crunching assessment data",
        caption = "We’re generating class-level analytics."
    ) {
        Text("Preparing insights…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    SectionCard(
        title = "Something went wrong",
        subtitle = "We couldn’t load the analytics",
        caption = message,
        trailingContent = {
            InfoPill(
                text = "Check connection",
                backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.error
            )
        }
    ) {
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyState() {
    SectionCard(
        title = "No report yet",
        subtitle = "Finish a session to unlock analytics",
        caption = "Deliver the module live or via assignment to populate this view."
    ) {
        Text("Complete assessments first.")
    }
}

@Composable
private fun OverviewCard(report: ClassReport) {
    val gain = (report.postAverage - report.preAverage).roundToInt()
    SectionCard(
        title = "Class overview",
        subtitle = "Learning gain snapshot",
        caption = "Track how the cohort moved from pre to post."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val overviewScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(overviewScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoPill(text = "Pre ${"%.1f".format(report.preAverage)}%")
                InfoPill(text = "Post ${"%.1f".format(report.postAverage)}%", backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.tertiary)
                InfoPill(
                    text = if (gain >= 0) "▲ +$gain pts" else "▼ ${(-gain)} pts",
                    backgroundColor = if (gain >= 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    contentColor = if (gain >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            LinearProgressIndicator(
                progress = { (report.postAverage / 100).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun MasteryCard(report: ClassReport) {
    SectionCard(
        title = "Objective mastery",
        subtitle = "Focus areas for reteach",
        caption = "Use this to celebrate wins and plan interventions."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            report.objectiveMastery.values.forEach { mastery ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(mastery.objective, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(
                            progress = { (mastery.post / 100).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.background
                        )
                        Text(
                            text = "Pre ${"%.1f".format(mastery.pre)}% → Post ${"%.1f".format(mastery.post)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttemptsCard(report: ClassReport) {
    SectionCard(
        title = "Learner attempts",
        subtitle = "Individual growth moments",
        caption = "Sort the class roster by post-test gains to spotlight growth."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            report.attempts.forEach { summary ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(summary.student.displayName, fontWeight = FontWeight.SemiBold)
                        val attemptScroll = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(attemptScroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            summary.prePercent?.let { InfoPill(text = "Pre ${"%.1f".format(it)}%") }
                            summary.postPercent?.let { InfoPill(text = "Post ${"%.1f".format(it)}%", backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.tertiary) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportCard(onExportPdf: () -> Unit, onExportCsv: () -> Unit) {
    SectionCard(
        title = "Exports",
        subtitle = "Share-ready reports",
        caption = "Send polished summaries to parents or fellow teachers."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onExportPdf,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.PictureAsPdf, contentDescription = null)
                Text("Export class PDF", modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = onExportCsv,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(imageVector = Icons.Rounded.TableChart, contentDescription = null)
                Text("Export CSV", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
