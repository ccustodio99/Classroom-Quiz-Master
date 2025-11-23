package com.classroom.quizmaster.ui.student.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.util.switchMapLatest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class StudentAssignmentFilter(val label: String) { Active("Active"), Completed("Completed") }

data class StudentAssignmentsUiState(
    val filter: StudentAssignmentFilter = StudentAssignmentFilter.Active,
    val active: List<AssignmentCardUi> = emptyList(),
    val completed: List<AssignmentCardUi> = emptyList(),
    val isRefreshing: Boolean = false
)

@HiltViewModel
class StudentAssignmentsViewModel @Inject constructor(
    private val assignmentRepository: AssignmentRepository,
    private val classroomRepository: ClassroomRepository,
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val filter = MutableStateFlow(StudentAssignmentFilter.Active)
    private val refreshing = MutableStateFlow(false)
    private val userSubmissions = authRepository.authState
        .switchMapLatest { auth ->
            val uid = auth.userId
            if (uid.isNullOrBlank()) flowOf(emptyList()) else assignmentRepository.submissionsForUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val baseState = combine(
        assignmentRepository.assignments,
        classroomRepository.classrooms,
        quizRepository.quizzes,
        userSubmissions,
        filter
    ) { assignments, classrooms, quizzes, submissionsForUser, selectedFilter ->
        val now = Clock.System.now()
        val quizTitles = quizzes.associate { it.id to it.title }
        val allowedClassrooms = classrooms.filterNot { it.isArchived }.map { it.id }.toSet()

        val cards = assignments
            .filter { !it.isArchived && it.classroomId in allowedClassrooms }
            .map { assignment ->
                val submission = submissionsForUser.firstOrNull { sub -> sub.assignmentId == assignment.id }
                val status = when {
                    submission != null -> "Submitted"
                    now < assignment.openAt -> "Scheduled"
                    now <= assignment.closeAt -> "Open"
                    else -> "Closed"
                }
                AssignmentCardUi(
                    id = assignment.id,
                    title = quizTitles[assignment.quizId] ?: "Assignment",
                    dueIn = formatDue(now, assignment.closeAt),
                    submissions = submission?.attempts ?: 0,
                    attemptsAllowed = assignment.attemptsAllowed,
                    statusLabel = status
                )
            }

        val active = cards.filter { it.statusLabel == "Open" || it.statusLabel == "Scheduled" }
        val completed = cards - active.toSet()

        StudentAssignmentsUiState(
            filter = selectedFilter,
            active = active,
            completed = completed,
            isRefreshing = false
        )
    }

    val uiState: StateFlow<StudentAssignmentsUiState> = combine(
        baseState,
        refreshing
    ) { state, isRefreshing ->
        state.copy(isRefreshing = isRefreshing)
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

    private fun formatDue(now: Instant, closeAt: Instant): String {
        return when {
            closeAt <= now -> "Closed"
            else -> {
                val hours = (closeAt - now).inWholeHours
                if (hours <= 48) "Due in ${hours}h" else "Due in ${(hours / 24)}d"
            }
        }
    }
}
