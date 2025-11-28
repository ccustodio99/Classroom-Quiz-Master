package com.classroom.quizmaster.ui.teacher.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.SectionCard
import com.classroom.quizmaster.ui.components.AssistiveInfoCard
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun ReportsRoute(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportsEvent.Snackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        when (val action = event.action) {
                            is SnackbarAction.OpenLink -> openUrl(context, action.url, snackbarHostState)
                            is SnackbarAction.Retry -> when (action.format) {
                                ExportFormat.CSV -> viewModel.exportCsv()
                                ExportFormat.PDF -> viewModel.exportPdf()
                            }
                            null -> {}
                        }
                    }
                }
            }
        }
    }

    ReportsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        onExportCsv = viewModel::exportCsv,
        onExportPdf = viewModel::exportPdf,
        onClassroomSelected = viewModel::onClassroomSelected
    )
}

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onClassroomSelected: (String?) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderSection(
                    classroomName = state.classroomName,
                    updatedLabel = state.lastUpdatedLabel,
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    classroomOptions = state.classroomOptions,
                    selectedClassroomId = state.selectedClassroomId,
                    onClassroomSelected = onClassroomSelected
                )
            }
            if (state.isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
            state.errorMessage?.let { error ->
                item {
                    AssistiveInfoCard(
                        title = "Unable to load reports",
                        body = error
                    )
                }
            }
            item { SummarySection(state.average, state.median) }
            item {
                ClassImprovementSection(
                    pre = state.classPreAverage,
                    post = state.classPostAverage,
                    delta = state.classDelta
                )
            }
            item {
                AssignmentCompletionSection(
                    overview = state.completionOverview,
                    assignments = state.assignmentCompletion,
                    students = state.studentCompletion
                )
            }
            item { StudentAtRiskSection(state.studentProgressAtRisk) }
            item { TopicMasterySection(state.topTopics) }
            item { AssignmentPerformanceSection(state.assignments) }
            item { StudentProgressSection(state.studentProgress) }
            item { StudentImprovementSection(state.studentImprovement) }
            item { QuestionDifficultySection(state.questionDifficulty) }
            item {
                ExportSection(
                    exportState = state.exportState,
                    onExportCsv = onExportCsv,
                    onExportPdf = onExportPdf
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    classroomName: String,
    updatedLabel: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    classroomOptions: List<ReportClassroomOption>,
    selectedClassroomId: String?,
    onClassroomSelected: (String?) -> Unit
) {
    val resolvedLabel = updatedLabel.ifBlank { "Not updated yet" }
    val selectedOption = classroomOptions.firstOrNull { it.id == selectedClassroomId } ?: classroomOptions.firstOrNull()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = classroomName.ifBlank { "Reports" },
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Updated $resolvedLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ClassroomSelector(
                selected = selectedOption,
                options = classroomOptions,
                onSelected = { option -> onClassroomSelected(option?.id) }
            )
        }
        PrimaryButton(
            text = if (isRefreshing) "Refreshing..." else "Refresh",
            onClick = onRefresh,
            enabled = !isRefreshing,
            isLoading = isRefreshing,
            leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Refresh, contentDescription = "Refresh") }
        )
    }
}

@Composable
private fun ClassroomSelector(
    selected: ReportClassroomOption?,
    options: List<ReportClassroomOption>,
    onSelected: (ReportClassroomOption?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        Surface(
            modifier = Modifier
                .padding(top = 4.dp),
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = selected?.name ?: "All classrooms",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                androidx.compose.material3.Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Select classroom")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun SummarySection(average: Int, median: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Average score",
            value = "$average%",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Median score",
            value = "$median%",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ClassImprovementSection(pre: Int, post: Int, delta: Int) {
    SectionCard(
        title = "Class pre/post",
        subtitle = "Growth across diagnostic tests"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Pre-test",
                value = "$pre%",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Post-test",
                value = "$post%",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Delta",
                value = formatDelta(delta),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TopicMasterySection(topics: List<TopicMasteryUi>) {
    SectionCard(
        title = "Topic mastery",
        subtitle = "Top areas by average best score"
    ) {
        if (topics.isEmpty()) {
            Text(
                text = "No topic performance yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topics.take(3).forEach { topic ->
                    TopicRow(topic)
                }
            }
        }
    }
}

@Composable
private fun AssignmentCompletionSection(
    overview: CompletionOverview,
    assignments: List<AssignmentCompletionUi>,
    students: List<StudentCompletionUi>
) {
    SectionCard(
        title = "Assignment completion",
        subtitle = "On-time, late, and missing work"
    ) {
        if (assignments.isEmpty() && students.isEmpty()) {
            EmptyState(
                title = "No submissions yet",
                message = "Once students start submitting, completion details will show here."
            )
            return@SectionCard
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "On-time",
                value = "${overview.onTimeRate}%",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Late",
                value = "${overview.lateRate}%",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Not attempted",
                value = "${overview.notAttemptedRate}%",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "By assignment",
            style = MaterialTheme.typography.titleMedium
        )
        if (assignments.isEmpty()) {
            Text(
                text = "No assignments to show.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                assignments.forEach { row -> AssignmentCompletionRow(row) }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "By student",
            style = MaterialTheme.typography.titleMedium
        )
        if (students.isEmpty()) {
            Text(
                text = "No student submissions yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                students.forEach { row -> StudentCompletionRow(row) }
            }
        }
    }
}

@Composable
private fun AssignmentCompletionRow(row: AssignmentCompletionUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                row.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${row.completedOnTime}/${row.totalStudents} on time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Late ${row.completedLate} | Not started ${row.notStarted}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
    }
}

@Composable
private fun StudentCompletionRow(row: StudentCompletionUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(row.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "On time ${row.completedOnTime} | Late ${row.completedLate} | Missing ${row.notAttempted} of ${row.totalAssignments}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
    }
}

private enum class ProgressFilter { ALL, NEEDS_SUPPORT, IMPROVING }

@Composable
private fun StudentAtRiskSection(rows: List<StudentProgressAtRiskUi>) {
    SectionCard(
        title = "Student progress & support",
        subtitle = "Average scores, trends, and missing work"
    ) {
        if (rows.isEmpty()) {
            Text(
                text = "No student data yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@SectionCard
        }
        var filter by remember { mutableStateOf(ProgressFilter.ALL) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == ProgressFilter.ALL,
                onClick = { filter = ProgressFilter.ALL },
                label = { Text("All") }
            )
            FilterChip(
                selected = filter == ProgressFilter.NEEDS_SUPPORT,
                onClick = { filter = ProgressFilter.NEEDS_SUPPORT },
                label = { Text("Needs support") }
            )
            FilterChip(
                selected = filter == ProgressFilter.IMPROVING,
                onClick = { filter = ProgressFilter.IMPROVING },
                label = { Text("Improving") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val filtered = when (filter) {
            ProgressFilter.ALL -> rows
            ProgressFilter.NEEDS_SUPPORT -> rows.filter { it.atRisk }
            ProgressFilter.IMPROVING -> rows.filter { it.trend == StudentTrend.IMPROVING }
        }
        if (filtered.isEmpty()) {
            Text(
                text = "No students in this filter yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filtered.forEach { row -> StudentAtRiskRow(row) }
            }
        }
    }
}

@Composable
private fun StudentAtRiskRow(row: StudentProgressAtRiskUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (row.atRisk) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    label = { Text("Needs support") }
                )
            }
        }
        Text(
            text = "Avg ${row.averageScore}% | Completed ${row.completedCount} | Missing ${row.missingCount}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Trend: ${row.trend.toLabel()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
    }
}

private enum class DifficultyFilter { ALL, EASY, HARD, CONFUSING }

@Composable
private fun QuestionDifficultySection(rows: List<QuestionDifficultyUi>) {
    SectionCard(
        title = "Question difficulty",
        subtitle = "Correct rates and confusing items"
    ) {
        if (rows.isEmpty()) {
            Text(
                text = "Not enough data to evaluate questions yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@SectionCard
        }
        var filter by remember { mutableStateOf(DifficultyFilter.ALL) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == DifficultyFilter.ALL,
                onClick = { filter = DifficultyFilter.ALL },
                label = { Text("All") }
            )
            FilterChip(
                selected = filter == DifficultyFilter.EASY,
                onClick = { filter = DifficultyFilter.EASY },
                label = { Text("Too easy") }
            )
            FilterChip(
                selected = filter == DifficultyFilter.HARD,
                onClick = { filter = DifficultyFilter.HARD },
                label = { Text("Too hard") }
            )
            FilterChip(
                selected = filter == DifficultyFilter.CONFUSING,
                onClick = { filter = DifficultyFilter.CONFUSING },
                label = { Text("Confusing") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val filtered = when (filter) {
            DifficultyFilter.ALL -> rows
            DifficultyFilter.EASY -> rows.filter { it.difficultyTag == QuestionDifficultyTag.TOO_EASY }
            DifficultyFilter.HARD -> rows.filter { it.difficultyTag == QuestionDifficultyTag.TOO_HARD }
            DifficultyFilter.CONFUSING -> rows.filter { it.difficultyTag == QuestionDifficultyTag.CONFUSING }
        }
        if (filtered.isEmpty()) {
            Text(
                text = "No questions match this filter yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filtered.forEach { row -> QuestionDifficultyRow(row) }
            }
        }
    }
}

@Composable
private fun QuestionDifficultyRow(row: QuestionDifficultyUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            row.questionPreview,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${row.assignmentTitle} | Correct ${row.pValue}% | Attempts ${row.attemptCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tag: ${row.difficultyTag.toLabel()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        row.topWrongOptionLabel?.let { option ->
            Text(
                text = "Top wrong answer: $option${row.topWrongOptionRate?.let { rate -> " ($rate%)" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider()
    }
}

private fun StudentTrend.toLabel(): String = when (this) {
    StudentTrend.IMPROVING -> "Improving"
    StudentTrend.STABLE -> "Stable"
    StudentTrend.DECLINING -> "Declining"
    StudentTrend.UNKNOWN -> "Not enough data yet"
}

private fun QuestionDifficultyTag.toLabel(): String = when (this) {
    QuestionDifficultyTag.TOO_EASY -> "Too easy"
    QuestionDifficultyTag.TOO_HARD -> "Too hard"
    QuestionDifficultyTag.CONFUSING -> "Confusing"
    QuestionDifficultyTag.NORMAL -> "Normal"
    QuestionDifficultyTag.INSUFFICIENT_DATA -> "Not enough data"
}

@Composable
private fun AssignmentPerformanceSection(rows: List<AssignmentPerformanceUi>) {
    SectionCard(
        title = "Assignments & quizzes",
        subtitle = "Difficulty (p-value) and distractors"
    ) {
        if (rows.isEmpty()) {
            EmptyState(
                title = "No attempts yet",
                message = "Once students submit, item difficulty will appear here."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    AssignmentRow(row)
                }
            }
        }
    }
}

@Composable
private fun StudentProgressSection(rows: List<StudentProgressUi>) {
    SectionCard(
        title = "Student progress",
        subtitle = "Completion and average best score"
    ) {
        if (rows.isEmpty()) {
            Text(
                text = "No student submissions yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    StudentProgressRow(row)
                }
            }
        }
    }
}

@Composable
private fun StudentImprovementSection(rows: List<StudentImprovementUi>) {
    SectionCard(
        title = "Pre/Post improvement",
        subtitle = "Students with both attempts"
    ) {
        if (rows.isEmpty()) {
            Text(
                text = "Not enough data yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    StudentImprovementRow(row)
                }
            }
        }
    }
}

@Composable
private fun ExportSection(
    exportState: ExportState,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit
) {
    SectionCard(
        title = "Export",
        subtitle = "Download analysis-ready CSV or a printable PDF report"
    ) {
        val isCsvLoading = exportState.isExporting && exportState.format == ExportFormat.CSV
        val isPdfLoading = exportState.isExporting && exportState.format == ExportFormat.PDF
        val noData = exportState.lastExportError?.contains("submissions", ignoreCase = true) == true
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "CSV • Download data for analysis",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryButton(
                    text = if (isCsvLoading) "Exporting..." else "Export CSV",
                    onClick = onExportCsv,
                    enabled = !exportState.isExporting && !noData,
                    isLoading = isCsvLoading,
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = "Export CSV"
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "PDF • Download printable report",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryButton(
                    text = if (isPdfLoading) "Exporting..." else "Export PDF",
                    onClick = onExportPdf,
                    enabled = !exportState.isExporting && !noData,
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.PictureAsPdf,
                            contentDescription = "Export PDF"
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                if (isPdfLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            if (exportState.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                exportState.lastCsvUrl?.let {
                    Text(
                        text = "Last CSV ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                exportState.lastPdfUrl?.let {
                    Text(
                        text = "Last PDF ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                exportState.lastExportError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } ?: run {
                    if (noData) {
                        Text(
                            text = "Exports will be available once there are submissions.",
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
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TopicRow(topic: TopicMasteryUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(topic.topicName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${topic.averageScore}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = (topic.averageScore.coerceIn(0, 100) / 100f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AssignmentRow(row: AssignmentPerformanceUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Outlined.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(row.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${row.pValue}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Text(
            text = "Top distractor: ${row.topDistractor} (${row.distractorRate}%)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
    }
}

@Composable
private fun StudentProgressRow(row: StudentProgressUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(row.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Completed ${row.completed} of ${row.total} | Avg score ${row.score}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StudentImprovementRow(row: StudentImprovementUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(row.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Pre ${row.preAvg}% | Post ${row.postAvg}% | Gain ${formatDelta(row.delta)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Attempts pre ${row.preAttempts}, post ${row.postAttempts}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDelta(delta: Int): String = if (delta >= 0) "+${delta}%" else "${delta}%"

private suspend fun openUrl(context: Context, url: String, snackbarHostState: SnackbarHostState) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            snackbarHostState.showSnackbar("Unable to open export link")
        }
}

@QuizPreviews
@Composable
private fun ReportsPreview() {
    QuizMasterTheme {
        ReportsScreen(
            state = ReportsUiState(
                isLoading = false,
                classroomName = "Algebra 8B",
                lastUpdatedLabel = "2 min ago",
                classroomOptions = listOf(
                    ReportClassroomOption(null, "All classrooms"),
                    ReportClassroomOption("c1", "Algebra 8B"),
                    ReportClassroomOption("c2", "Biology 8C")
                ),
                selectedClassroomId = "c1",
                average = 82,
                median = 78,
                classPreAverage = 60,
                classPostAverage = 78,
                classDelta = 18,
                topTopics = listOf(
                    TopicMasteryUi("Fractions", 88),
                    TopicMasteryUi("Ecosystems", 76),
                    TopicMasteryUi("Grammar", 70)
                ),
                assignments = listOf(
                    AssignmentPerformanceUi("a1", "Quiz 1 - Fractions", 72),
                    AssignmentPerformanceUi("a2", "Quiz 2 - Decimals", 85)
                ),
                studentProgress = listOf(
                    StudentProgressUi("Alex Rivers", completed = 8, total = 10, score = 88),
                    StudentProgressUi("Jamie Lee", completed = 6, total = 10, score = 72)
                ),
                studentImprovement = listOf(
                    StudentImprovementUi("Alex Rivers", preAvg = 60, postAvg = 88, delta = 28, preAttempts = 1, postAttempts = 1),
                    StudentImprovementUi("Jamie Lee", preAvg = 55, postAvg = 70, delta = 15, preAttempts = 1, postAttempts = 1)
                ),
                completionOverview = CompletionOverview(onTimeRate = 72, lateRate = 18, notAttemptedRate = 10),
                assignmentCompletion = listOf(
                    AssignmentCompletionUi("a1", "Quiz 1 - Fractions", totalStudents = 25, completedOnTime = 18, completedLate = 3, notStarted = 4),
                    AssignmentCompletionUi("a2", "Quiz 2 - Decimals", totalStudents = 25, completedOnTime = 20, completedLate = 2, notStarted = 3)
                ),
                studentCompletion = listOf(
                    StudentCompletionUi("s1", "Alex Rivers", completedOnTime = 8, completedLate = 1, notAttempted = 1, totalAssignments = 10),
                    StudentCompletionUi("s2", "Jamie Lee", completedOnTime = 6, completedLate = 2, notAttempted = 2, totalAssignments = 10)
                ),
                studentProgressAtRisk = listOf(
                    StudentProgressAtRiskUi("s1", "Alex Rivers", averageScore = 88, trend = StudentTrend.IMPROVING, completedCount = 10, missingCount = 0, atRisk = false),
                    StudentProgressAtRiskUi("s2", "Jamie Lee", averageScore = 68, trend = StudentTrend.DECLINING, completedCount = 8, missingCount = 2, atRisk = true)
                ),
                questionDifficulty = listOf(
                    QuestionDifficultyUi("q1", "Quiz 1 - Fractions", "Add fractions with like denominators", 72, null, null, QuestionDifficultyTag.NORMAL, attemptCount = 18),
                    QuestionDifficultyUi("q2", "Quiz 1 - Fractions", "Simplify the fraction 12/16", 28, null, null, QuestionDifficultyTag.TOO_HARD, attemptCount = 18),
                    QuestionDifficultyUi("q3", "Quiz 2 - Decimals", "Round 3.146 to the nearest tenth", 94, null, null, QuestionDifficultyTag.TOO_EASY, attemptCount = 20)
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onRefresh = {},
            onExportCsv = {},
            onExportPdf = {},
            onClassroomSelected = {}
        )
    }
}
