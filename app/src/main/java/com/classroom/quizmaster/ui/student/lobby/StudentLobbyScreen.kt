package com.classroom.quizmaster.ui.student.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

data class StudentLobbyUiState(
    val studentId: String = "",
    val hostName: String = "",
    val joinStatus: String = "Waiting for host",
    val avatars: List<AvatarOption> = emptyList(),
    val ready: Boolean = false,
    val lockedMessage: String? = null
)

@HiltViewModel
class StudentLobbyViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {
    private val _uiState = MutableStateFlow(StudentLobbyUiState())
    val uiState: StateFlow<StudentLobbyUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentLobby.collect { _uiState.value = it }
        }
    }

    fun toggleReady() {
        val current = _uiState.value
        val next = !current.ready
        _uiState.update { it.copy(ready = next) }
        viewModelScope.launch {
            sessionRepositoryUi.toggleReady(current.studentId)
        }
    }
}

@Composable
fun StudentLobbyRoute(
    onReady: () -> Unit,
    viewModel: StudentLobbyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudentLobbyScreen(state = state, onToggleReady = {
        viewModel.toggleReady()
        onReady()
    })
}

@Composable
fun StudentLobbyScreen(
    state: StudentLobbyUiState,
    onToggleReady: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Host: ${state.hostName}", style = MaterialTheme.typography.titleLarge)
        Text(state.joinStatus, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.avatars.forEach {
                TagChip(text = it.label)
            }
        }
        state.lockedMessage?.let {
            TagChip(text = it)
        }
        PrimaryButton(
            text = if (state.ready) "Ready!" else "Tap when ready",
            onClick = onToggleReady
        )
    }
}

@QuizPreviews
@Composable
private fun StudentLobbyPreview() {
    QuizMasterTheme {
        StudentLobbyScreen(
            state = StudentLobbyUiState(
                hostName = "Ms. Navarro",
                joinStatus = "Waiting for 2 more players",
                avatars = listOf(
                    AvatarOption("1", "Nova", emptyList(), "spark"),
                    AvatarOption("2", "Bolt", emptyList(), "atom")
                ),
                lockedMessage = "Host locked joins after Q1"
            ),
            onToggleReady = {}
        )
    }
}
