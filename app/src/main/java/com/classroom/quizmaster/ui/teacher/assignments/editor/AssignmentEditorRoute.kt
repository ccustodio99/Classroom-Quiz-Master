package com.classroom.quizmaster.ui.teacher.assignments.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SegmentOption
import com.classroom.quizmaster.ui.components.SegmentedControl
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.ToggleChip
import java.time.format.DateTimeFormatter
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun AssignmentEditorRoute(
    onDone: () -> Unit,
    onArchived: () -> Unit,
    viewModel: AssignmentEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onDone()
    }
    LaunchedEffect(state.archiveSuccess) {
        if (state.archiveSuccess) onArchived()
    }
    AssignmentEditorScreen(
        state = state,
        onBack = onDone,
        onSelectQuiz = viewModel::updateQuiz,
        onOpenChanged = viewModel::updateOpenAt,
        onCloseChanged = viewModel::updateCloseAt,
        onAttemptsChanged = viewModel::updateAttempts,
        onScoringModeChanged = viewModel::updateScoringMode,
        onRevealChanged = viewModel::updateRevealAfterSubmit,
        onSave = viewModel::save,
        onArchive = viewModel::archive
    )
}

@Composable
fun AssignmentEditorScreen(
    state: AssignmentEditorUiState,
    onBack: () -> Unit,
    onSelectQuiz: (String) -> Unit,
    onOpenChanged: (Instant) -> Unit,
    onCloseChanged: (Instant) -> Unit,
    onAttemptsChanged: (String) -> Unit,
    onScoringModeChanged: (ScoringMode) -> Unit,
    onRevealChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val canSave = state.selectedQuizId.isNotBlank() && state.openAt != null && state.closeAt != null && !state.isSaving
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SecondaryButton(text = "Back", onClick = onBack)
        Text(
            text = if (state.mode == EditorMode.Create) "Assign a quiz" else "Edit assignment",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = if (state.mode == EditorMode.Create) {
                "Choose a quiz, schedule availability, and share it for independent practice."
            } else {
                "Update the schedule or scoring rules, or archive the assignment if it's no longer needed."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.quizOptions.isEmpty()) {
            EmptyState(
                title = "No quizzes available",
                message = "Create a quiz for this topic before assigning it."
            )
        } else {
            QuizPicker(
                options = state.quizOptions,
                selectedId = state.selectedQuizId,
                enabled = !state.isSaving,
                onSelected = onSelectQuiz
            )
        }
        DateTimeField(
            label = "Opens",
            instant = state.openAt,
            timeZone = timeZone,
            enabled = !state.isSaving,
            onInstantChanged = onOpenChanged
        )
        DateTimeField(
            label = "Closes",
            instant = state.closeAt,
            timeZone = timeZone,
            enabled = !state.isSaving,
            onInstantChanged = onCloseChanged
        )
        OutlinedTextField(
            value = state.attemptsAllowed,
            onValueChange = onAttemptsChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Attempts allowed") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = !state.isSaving
        )
        Text(text = "Scoring mode", style = MaterialTheme.typography.titleMedium)
        SegmentedControl(
            options = scoringOptions(),
            selectedId = state.scoringMode.name,
            onSelected = { id ->
                val mode = runCatching { ScoringMode.valueOf(id) }.getOrDefault(ScoringMode.BEST)
                onScoringModeChanged(mode)
            }
        )
        ToggleChip(
            label = "Reveal answers after submission",
            checked = state.revealAfterSubmit,
            onCheckedChange = { onRevealChanged(it) },
            description = "Students can review correct answers after turning in the assignment."
        )
        state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = onBack,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            )
            PrimaryButton(
                text = if (state.mode == EditorMode.Create) "Assign" else "Save",
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.weight(1f)
            )
        }
        if (state.canArchive) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Archive",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Archiving hides this assignment from active lists but keeps the data for reference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PrimaryButton(
                    text = "Archive assignment",
                    onClick = onArchive,
                    enabled = !state.isSaving
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun QuizPicker(
    options: List<QuizOptionUi>,
    selectedId: String,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Quiz", style = MaterialTheme.typography.titleMedium)
        options.forEach { option ->
            val selected = option.id == selectedId
            val label = if (selected) "${option.title} • Selected" else option.title
            SecondaryButton(
                text = label,
                onClick = { onSelected(option.id) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DateTimeField(
    label: String,
    instant: Instant?,
    timeZone: TimeZone,
    enabled: Boolean,
    onInstantChanged: (Instant) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a") }
    val display = instant?.let { instantToDisplay(it, timeZone, formatter) } ?: "Select date"
    val interactionModifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = enabled) {
            val initial = (instant ?: Clock.System.now()).toLocalDateTime(timeZone)
            val datePicker = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val timePicker = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val picked = LocalDateTime(year, month + 1, dayOfMonth, hourOfDay, minute)
                            onInstantChanged(picked.toInstant(timeZone))
                        },
                        initial.hour,
                        initial.minute,
                        false
                    )
                    timePicker.show()
                },
                initial.year,
                initial.monthNumber - 1,
                initial.dayOfMonth
            )
            datePicker.show()
        }
    OutlinedTextField(
        value = display,
        onValueChange = {},
        modifier = interactionModifier,
        label = { Text(label) },
        readOnly = true,
        enabled = enabled
    )
}

private fun scoringOptions(): List<SegmentOption> = listOf(
    SegmentOption(ScoringMode.BEST.name, "Best", "Keep the best score"),
    SegmentOption(ScoringMode.LAST.name, "Last", "Grade the last attempt"),
    SegmentOption(ScoringMode.AVERAGE.name, "Average", "Average attempts")
)

private fun instantToDisplay(
    instant: Instant,
    timeZone: TimeZone,
    formatter: DateTimeFormatter
): String {
    val local = instant.toLocalDateTime(timeZone)
    val javaLocal = java.time.LocalDateTime.of(
        local.year,
        local.monthNumber,
        local.dayOfMonth,
        local.hour,
        local.minute
    )
    return formatter.format(javaLocal)
}
