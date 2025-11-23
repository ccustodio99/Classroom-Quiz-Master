package com.classroom.quizmaster.ui.student.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudentAssignmentFilter(val label: String) { Active("Active"), Completed("Completed") }

data class StudentAssignmentsUiState(
    val filter: StudentAssignmentFilter = StudentAssignmentFilter.Active,
    val active: List<AssignmentCardUi> = emptyList(),
    val completed: List<AssignmentCardUi> = emptyList(),
    val isRefreshing: Boolean = false
)

@HiltViewModel
class StudentAssignmentsViewModel @Inject constructor(
    private val assignmentRepositoryUi: AssignmentRepositoryUi,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val filter = MutableStateFlow(StudentAssignmentFilter.Active)
    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<StudentAssignmentsUiState> = combine(
        assignmentRepositoryUi.assignments,
        filter,
        refreshing
    ) { assignments, selectedFilter, isRefreshing ->
        StudentAssignmentsUiState(
            filter = selectedFilter,
            active = assignments.pending,
            completed = assignments.archived,
            isRefreshing = isRefreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentAssignmentsUiState())

    fun selectFilter(newFilter: StudentAssignmentFilter) {
        filter.value = newFilter
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.update { true }
            runCatching { assignmentRepository.refreshAssignments() }
            refreshing.update { false }
        }
    }
}
