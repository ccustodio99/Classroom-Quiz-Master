package com.classroom.quizmaster.ui.student.end

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.components.LeaderboardList
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class StudentEndUiState(
    val stars: Int = 0,
    val rank: Int = 0,
    val team: String = "",
    val badges: List<String> = emptyList(),
    val totalScore: Int = 0,
    val improvement: Int = 0,
    val leaderboard: List<LeaderboardRowUi> = emptyList(),
    val summary: String = "Great job!"
)

@HiltViewModel
class StudentEndViewModel @Inject constructor(
    sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentEndUiState())
    val uiState: StateFlow<StudentEndUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentEnd.collectLatest { _uiState.value = it }
        }
    }
}

@Composable
fun StudentEndRoute(
    onPlayAgain: () -> Unit,
    onLeave: () -> Unit,
    viewModel: StudentEndViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentEndScreen(state = state, onPlayAgain = onPlayAgain, onLeave = onLeave)
}

@Composable
fun StudentEndScreen(
    state: StudentEndUiState,
    onPlayAgain: () -> Unit,
    onLeave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Session complete", style = MaterialTheme.typography.headlineMedium)
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 3.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(state.summary, style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    TagChip(text = "Stars ${state.stars}")
                    TagChip(text = "Rank #${state.rank}")
                    TagChip(text = "Team ${state.team}")
                }
                TagChip(text = "Score ${state.totalScore} (+${state.improvement} vs last time)")
                if (state.badges.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.badges.forEach { badge -> TagChip(text = badge) }
                    }
                }
            }
        }
        if (state.leaderboard.isNotEmpty()) {
            LeaderboardList(
                rows = state.leaderboard,
                modifier = Modifier.fillMaxWidth(),
                headline = "Top performers",
                compact = true
            )
        }
        PrimaryButton(text = "Play again", onClick = onPlayAgain)
        SecondaryButton(text = "Leave lobby", onClick = onLeave)
    }
}

@QuizPreviews
@Composable
private fun StudentEndPreview() {
    QuizMasterTheme {
        StudentEndScreen(
            state = StudentEndUiState(
                stars = 3,
                rank = 4,
                team = "Galaxy Owls",
                totalScore = 1220,
                improvement = 85,
                badges = listOf("Fast thinker", "Team captain"),
                leaderboard = listOf(
                    LeaderboardRowUi(1, "Nova", 1320, 28, AvatarOption("1", "N", emptyList(), "spark"), true),
                    LeaderboardRowUi(2, "Bolt", 1260, 15, AvatarOption("2", "B", emptyList(), "atom"), false)
                )
            ),
            onPlayAgain = {},
            onLeave = {}
        )
    }
}
