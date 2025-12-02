package com.classroom.quizmaster.ui.student.assignments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.ui.components.SimpleTopBar

@Composable
fun StudentAssignmentPlayRoute(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: StudentAssignmentPlayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }
    StudentAssignmentPlayScreen(
        state = state,
        onBack = onBack,
        onToggle = viewModel::toggleChoice,
        onFreeResponseChange = viewModel::updateFreeResponse,
        onNext = { viewModel.submitAndNext() }
    )
}

@Composable
fun StudentAssignmentPlayScreen(
    state: AssignmentPlayUiState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onFreeResponseChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val scrollState = rememberScrollState()
    val question = state.question
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SimpleTopBar(
                title = "Assignment",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            if (question == null) {
                Text("Loading questions...", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Text(
                text = "Question ${state.index} of ${state.total}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = question.stem,
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (question.type) {
                        QuestionType.FILL_IN -> {
                            OutlinedTextField(
                                value = state.freeResponse,
                                onValueChange = onFreeResponseChange,
                                label = { Text("Type your answer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            question.choices.forEach { choice ->
                                ChoiceRow(
                                    text = choice,
                                    selected = state.selected.contains(choice),
                                    onToggle = { onToggle(choice) }
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = when (question.type) {
                    QuestionType.MCQ, QuestionType.TF -> "Select the correct option(s)."
                    QuestionType.FILL_IN -> "Enter your answer; spelling must match the expected response."
                    else -> "This question type is best played live; select what you think is correct."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.finished) {
                Text("Submitted score: ${state.score}", color = MaterialTheme.colorScheme.primary)
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        val canAdvance = question != null && when (question.type) {
            QuestionType.FILL_IN -> state.freeResponse.isNotBlank()
            QuestionType.MCQ, QuestionType.TF -> state.selected.isNotEmpty()
            else -> true
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            enabled = !state.finished && canAdvance
        ) {
            Text(
                when {
                    state.finished -> "Submitted"
                    state.index >= state.total -> "Submit"
                    else -> "Next"
                }
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    text: String,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = selected, onValueChange = { onToggle() })
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
