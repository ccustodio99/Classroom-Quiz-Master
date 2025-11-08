package com.classroom.quizmaster.ui.student.end

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.state.SessionRepositoryUi

data class StudentEndUiState(
    val stars: Int = 0,
    val rank: Int = 0,
    val team: String = "",
    val badges: List<String> = emptyList()
)

@HiltViewModel
class StudentEndViewModel @Inject constructor(
    sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentEndUiState())
    val uiState: StateFlow<StudentEndUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentEnd.collect { _uiState.value = it }
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
        Text("Great job!", style = MaterialTheme.typography.headlineMedium)
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Stars earned: ${state.stars}")
                Text("Rank: #${state.rank}")
                Text("Team: ${state.team}")
                Text("Badges: ${state.badges.joinToString()}")
            }
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
            state = StudentEndUiState(stars = 3, rank = 4, team = "Galaxy Owls", badges = listOf("Fast Thinker")),
            onPlayAgain = {},
            onLeave = {}
        )
    }
}
