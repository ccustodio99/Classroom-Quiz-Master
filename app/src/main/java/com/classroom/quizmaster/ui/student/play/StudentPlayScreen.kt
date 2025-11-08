package com.classroom.quizmaster.ui.student.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.components.TimerRing
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun StudentPlayRoute(
    viewModel: StudentPlayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentPlayScreen(
        state = state,
        onAnswerSelected = viewModel::selectAnswer
    )
}

@Composable
fun StudentPlayScreen(
    state: StudentPlayUiState,
    onAnswerSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Question", style = MaterialTheme.typography.titleLarge)
                TagChip(text = if (state.reveal) "Answer revealed" else "Submit now")
            }
            TimerRing(
                progress = state.progress,
                remainingSeconds = state.timerSeconds
            )
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp
        ) {
            Text(
                text = state.question?.stem ?: "Waiting for host...",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            val answers = state.question?.answers.orEmpty()
            items(answers, key = { it.id }) { answer ->
                AnswerCard(
                    answer = answer,
                    selected = answer.id in state.selectedAnswers,
                    disabled = state.reveal,
                    onClick = { onAnswerSelected(answer.id) }
                )
            }
        }
        if (state.reveal) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = state.question?.explanation ?: "",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AnswerCard(
    answer: AnswerOptionUi,
    selected: Boolean,
    disabled: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        disabled && answer.correct -> MaterialTheme.colorScheme.tertiaryContainer
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !disabled) { onClick() },
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .background(background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(answer.label, style = MaterialTheme.typography.titleLarge)
            Text(answer.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@QuizPreviews
@Composable
private fun StudentPlayPreview() {
    QuizMasterTheme {
        StudentPlayScreen(
            state = StudentPlayUiState(
                question = QuestionDraftUi(
                    id = "q1",
                    stem = "Which planet is known as the Red Planet?",
                    type = QuestionTypeUi.MultipleChoice,
                    answers = listOf(
                        AnswerOptionUi("a1", "A", "Mars", true),
                        AnswerOptionUi("a2", "B", "Venus", false)
                    ),
                    explanation = "Mars dust is red."
                ),
                selectedAnswers = setOf("a2"),
                reveal = true,
                timerSeconds = 12,
                progress = 0.3f
            ),
            onAnswerSelected = {}
        )
    }
}
