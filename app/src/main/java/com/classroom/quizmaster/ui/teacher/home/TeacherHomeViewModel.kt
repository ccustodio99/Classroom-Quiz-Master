package com.classroom.quizmaster.ui.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherHomeUiState(
    val quizzes: List<Quiz> = emptyList(),
    val pendingOps: Int = 0,
    val isSyncing: Boolean = false
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    sessionRepository: SessionRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    val uiState: StateFlow<TeacherHomeUiState> = combine(
        quizRepository.quizzes,
        sessionRepository.pendingOpCount
    ) { quizzes, pending ->
        TeacherHomeUiState(
            quizzes = quizzes,
            pendingOps = pending,
            isSyncing = pending > 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeacherHomeUiState())

    fun refresh() {
        viewModelScope.launch { quizRepository.refresh() }
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
