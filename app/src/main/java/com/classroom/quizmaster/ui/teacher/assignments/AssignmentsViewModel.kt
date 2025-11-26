package com.classroom.quizmaster.ui.teacher.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

data class AssignmentsUiState(
    val pending: List<AssignmentCardUi> = emptyList(),
    val archived: List<AssignmentCardUi> = emptyList()
)

@HiltViewModel
class AssignmentsViewModel @Inject constructor(
    assignmentRepositoryUi: AssignmentRepositoryUi,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    val uiState: StateFlow<AssignmentsUiState> = assignmentRepositoryUi.assignments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssignmentsUiState())

    init {
        viewModelScope.launch {
            runCatching { assignmentRepository.refreshAssignments() }
                .onFailure { Timber.w(it, "Failed to refresh assignments") }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { assignmentRepository.refreshAssignments() }
                .onFailure { Timber.w(it, "Manual assignment refresh failed") }
        }
    }
}
