package com.classroom.quizmaster.ui.student.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.domain.usecase.SubmitAnswerUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentPlayUiState(
    val session: Session? = null,
    val timerSeconds: Int = 30,
    val selectedAnswers: Set<String> = emptySet(),
    val reveal: Boolean = false
)

@HiltViewModel
class StudentPlayViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentPlayUiState())
    val uiState: StateFlow<StudentPlayUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionRepository.session.collectLatest { session ->
                _uiState.value = _uiState.value.copy(
                    session = session,
                    reveal = session?.reveal == true
                )
            }
        }
    }

    fun submitAnswer(
        questionId: String,
        choices: List<String>,
        correct: List<String>,
        timeTaken: Long,
        timeLimit: Long,
        nonce: String,
        reveal: Boolean
    ) {
        viewModelScope.launch {
            val uid = firebaseAuth.currentUser?.uid ?: "guest-${System.currentTimeMillis()}"
            submitAnswerUseCase(
                uid = uid,
                questionId = questionId,
                selected = choices,
                correctAnswers = correct,
                timeTakenMs = timeTaken,
                timeLimitMs = timeLimit,
                nonce = nonce,
                revealHappened = reveal
            )
        }
    }
}
