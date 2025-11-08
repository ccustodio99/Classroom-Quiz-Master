package com.classroom.quizmaster.ui.teacher.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.ConfirmEndDialog
import com.classroom.quizmaster.ui.components.DistributionBarChart
import com.classroom.quizmaster.ui.components.LeaderboardList
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.components.TimerRing
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.DistributionBar
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun HostLiveRoute(
    onSessionEnded: () -> Unit,
    viewModel: HostLiveViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HostLiveScreen(
        state = state,
        onReveal = viewModel::reveal,
        onNext = viewModel::next,
        onToggleLeaderboard = viewModel::toggleLeaderboard,
        onToggleMute = viewModel::toggleMute,
        onEnd = {
            viewModel.endSession()
            onSessionEnded()
        }
    )
}

@Composable
fun HostLiveScreen(
    state: HostLiveUiState,
    onReveal: () -> Unit,
    onNext: () -> Unit,
    onToggleLeaderboard: (Boolean) -> Unit,
    onToggleMute: (Boolean) -> Unit,
    onEnd: () -> Unit
) {
    var showEndDialog by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val timeLimit = (state.question?.timeLimitSeconds ?: 60).coerceAtLeast(1)
        val progress = (state.timerSeconds.toFloat() / timeLimit.toFloat()).coerceIn(0f, 1f)
        val totalQuestions = state.totalQuestions.takeIf { it > 0 } ?: (state.questionIndex + 1).coerceAtLeast(1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Question ${state.questionIndex + 1}/$totalQuestions",
                    style = MaterialTheme.typography.titleLarge
                )
                TagChip(text = if (state.isRevealed) "Revealed" else "Live")
            }
            TimerRing(progress = progress, remainingSeconds = state.timerSeconds)
        }
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = state.question?.stem ?: "Waiting for next question")
                state.question?.answers?.forEach { answer ->
                    Text("${answer.label}. ${answer.text}")
                }
            }
        }
        if (state.isRevealed) {
            DistributionBarChart(data = state.distribution)
            Text(
                text = state.question?.explanation.orEmpty(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (state.showLeaderboard) {
            LeaderboardList(
                rows = state.leaderboard,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TagChip(
                text = "Leaderboard hidden for students",
                modifier = Modifier.fillMaxWidth()
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val buttonWidth = (maxWidth - 12.dp) / 2
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = if (state.isRevealed) "Next question" else "Reveal",
                    onClick = if (state.isRevealed) onNext else onReveal,
                    modifier = Modifier.width(buttonWidth)
                )
                SecondaryButton(
                    text = if (state.showLeaderboard) "Hide leaderboard" else "Show leaderboard",
                    onClick = { onToggleLeaderboard(!state.showLeaderboard) },
                    modifier = Modifier.width(buttonWidth)
                )
            }
        }
        SecondaryButton(
            text = if (state.muteSfx) "Enable sound cues" else "Mute sound cues",
            onClick = { onToggleMute(!state.muteSfx) },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { showEndDialog = true }) {
            Text("End session")
        }
    }

    ConfirmEndDialog(
        open = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            onEnd()
        }
    )
}

@QuizPreviews
@Composable
private fun HostLivePreview() {
    QuizMasterTheme {
        HostLiveScreen(
            state = HostLiveUiState(
                questionIndex = 3,
                totalQuestions = 10,
                timerSeconds = 24,
                isRevealed = true,
                question = QuestionDraftUi(
                    id = "q1",
                    stem = "Which planet is known as the Red Planet?",
                    type = QuestionTypeUi.MultipleChoice,
                    answers = listOf(
                        AnswerOptionUi("a", "A", "Mars", true),
                        AnswerOptionUi("b", "B", "Venus", false)
                    ),
                    explanation = "Iron oxide dust gives Mars a red hue."
                ),
                distribution = listOf(
                    DistributionBar("A", 0.8f, true),
                    DistributionBar("B", 0.1f),
                    DistributionBar("C", 0.1f)
                ),
                leaderboard = listOf(
                    LeaderboardRowUi(1, "Ava", 980, 12, AvatarOption("1", "A", emptyList(), "spark"), true),
                    LeaderboardRowUi(2, "Liam", 950, -5, AvatarOption("2", "L", emptyList(), "atom"))
                )
            ),
            onReveal = {},
            onNext = {},
            onToggleLeaderboard = {},
            onToggleMute = {},
            onEnd = {}
        )
    }
}
