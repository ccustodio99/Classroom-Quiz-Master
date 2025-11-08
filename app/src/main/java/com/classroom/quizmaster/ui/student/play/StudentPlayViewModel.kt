package com.classroom.quizmaster.ui.student.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentPlayUiState(
    val question: QuestionDraftUi? = null,
    val timerSeconds: Int = 30,
    val selectedAnswers: Set<String> = emptySet(),
    val feedback: String? = null,
    val reveal: Boolean = false,
    val progress: Float = 0f
)

@HiltViewModel
class StudentPlayViewModel @Inject constructor(
    private val sessionRepositoryUi: SessionRepositoryUi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentPlayUiState())
    val uiState: StateFlow<StudentPlayUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepositoryUi.studentPlay.collect { incoming ->
                _uiState.value = incoming
            }
        }
    }

    fun selectAnswer(answerId: String) {
        val question = _uiState.value.question ?: return
        _uiState.update { state ->
            val next = if (question.type.name == "MultipleChoice" || question.type.name == "TrueFalse") {
                setOf(answerId)
            } else {
                state.selectedAnswers.toMutableSet().apply {
                    if (contains(answerId)) remove(answerId) else add(answerId)
                }
            }
            state.copy(selectedAnswers = next)
        }
    }
}
