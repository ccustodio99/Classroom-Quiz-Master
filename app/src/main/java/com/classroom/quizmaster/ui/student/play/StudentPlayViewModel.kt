package com.classroom.quizmaster.ui.student.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.model.ConnectionQuality
import com.classroom.quizmaster.ui.model.DistributionBar
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SubmissionStatus { Idle, Sending, Acknowledged, Late, Error }

data class StudentPlayUiState(
    val question: QuestionDraftUi? = null,
    val timerSeconds: Int = 30,
    val selectedAnswers: Set<String> = emptySet(),
    val reveal: Boolean = false,
    val progress: Float = 0f,
    val leaderboard: List<LeaderboardRowUi> = emptyList(),
    val distribution: List<DistributionBar> = emptyList(),
    val streak: Int = 0,
    val totalScore: Int = 0,
    val latencyMs: Int = 0,
    val connectionQuality: ConnectionQuality = ConnectionQuality.Good,
    val submissionStatus: SubmissionStatus = SubmissionStatus.Idle,
    val submissionMessage: String = "",
    val showLeaderboard: Boolean = false,
    val requiresManualSubmit: Boolean = false
)

@HiltViewModel
class StudentPlayViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentPlayUiState())
    val uiState: StateFlow<StudentPlayUiState> = _uiState

    private var lastQuestionId: String? = null

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentPlay.collectLatest { incoming ->
                _uiState.update { current ->
                    val nextQuestionId = incoming.question?.id
                    val resetSelection = nextQuestionId != null && nextQuestionId != lastQuestionId
                    if (resetSelection) {
                        lastQuestionId = nextQuestionId
                    }
                    val selectedAnswers = if (resetSelection) emptySet() else current.selectedAnswers
                    current.copy(
                        question = incoming.question,
                        timerSeconds = incoming.timerSeconds,
                        reveal = incoming.reveal,
                        progress = incoming.progress,
                        leaderboard = incoming.leaderboard,
                        distribution = incoming.distribution,
                        streak = incoming.streak,
                        totalScore = incoming.totalScore,
                        latencyMs = incoming.latencyMs,
                        connectionQuality = incoming.connectionQuality,
                        submissionStatus = incoming.submissionStatus,
                        submissionMessage = incoming.submissionMessage,
                        selectedAnswers = if (incoming.reveal) selectedAnswers else selectedAnswers,
                        requiresManualSubmit = incoming.requiresManualSubmit
                    )
                }
            }
        }
    }

    fun toggleLeaderboard() {
        _uiState.update { it.copy(showLeaderboard = !it.showLeaderboard) }
    }

    fun syncSession() {
        viewModelScope.launch {
            sessionRepositoryUi.syncSession()
        }
    }

    fun selectAnswer(answerId: String) {
        val question = _uiState.value.question ?: return
        if (_uiState.value.reveal) return
        val updatedSelection = when (question.type) {
            QuestionTypeUi.MultipleChoice, QuestionTypeUi.TrueFalse -> setOf(answerId)
            else -> _uiState.value.selectedAnswers.toMutableSet().apply {
                if (!add(answerId)) remove(answerId)
            }
        }
        _uiState.update { it.copy(selectedAnswers = updatedSelection) }
        if (!requiresManualSubmit(question.type)) {
            submitSelection()
        }
    }

    fun submitSelection() {
        val selection = _uiState.value.selectedAnswers
        if (selection.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(submissionStatus = SubmissionStatus.Sending, submissionMessage = "Submitting...") }
            runCatching { sessionRepositoryUi.submitStudentAnswer(selection.toList()) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            submissionStatus = SubmissionStatus.Acknowledged,
                            submissionMessage = ""
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            submissionStatus = SubmissionStatus.Error,
                            submissionMessage = throwable.message ?: "Failed to submit"
                        )
                    }
                }
        }
    }

    private fun requiresManualSubmit(type: QuestionTypeUi): Boolean =
        type == QuestionTypeUi.FillIn || type == QuestionTypeUi.Match
}
