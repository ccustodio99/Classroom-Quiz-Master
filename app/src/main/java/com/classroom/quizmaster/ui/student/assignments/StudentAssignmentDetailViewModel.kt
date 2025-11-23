package com.classroom.quizmaster.ui.student.assignments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.usecase.SubmitAssignmentAttemptUseCase
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
import kotlinx.coroutines.flow.first
import com.classroom.quizmaster.util.switchMapLatest

data class StudentAssignmentDetailUiState(
    val assignmentId: String = "",
    val title: String = "",
    val statusLabel: String = "",
    val dueLabel: String = "",
    val attemptsUsed: Int = 0,
    val attemptsAllowed: Int = 0,
    val canStart: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class StudentAssignmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val assignmentRepository: AssignmentRepository,
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository,
    private val submitAssignmentAttemptUseCase: SubmitAssignmentAttemptUseCase
) : ViewModel() {

    private val assignmentId: String = savedStateHandle["assignmentId"] ?: ""
    private val submitting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val userSubmissions = authRepository.authState.switchMapLatest { auth ->
        val uid = auth.userId
        if (uid.isNullOrBlank()) flowOf(emptyList()) else assignmentRepository.submissionsForUser(uid)
    }

    private val baseState = combine(
        assignmentRepository.assignments,
        quizRepository.quizzes,
        userSubmissions,
        authRepository.authState
    ) { assignments, quizzes, submissions, auth ->
        val now = Clock.System.now()
        val assignment = assignments.firstOrNull { it.id == assignmentId }
        val quizTitle = quizzes.firstOrNull { it.id == assignment?.quizId }?.title ?: "Assignment"
        val submission = submissions.firstOrNull { it.assignmentId == assignmentId }
        val status = when {
            assignment == null -> "Not found"
            submission != null -> "Submitted"
            now < assignment.openAt -> "Scheduled"
            now <= assignment.closeAt -> "Open"
            else -> "Closed"
        }
        val attemptsUsed = submission?.attempts ?: 0
        val canStart = assignment != null &&
            status == "Open" &&
            attemptsUsed < assignment.attemptsAllowed &&
            !auth.userId.isNullOrBlank()

        StudentAssignmentDetailUiState(
            assignmentId = assignmentId,
            title = quizTitle,
            statusLabel = status,
            dueLabel = assignment?.let { dueLabel(now, it.closeAt) } ?: "",
            attemptsUsed = attemptsUsed,
            attemptsAllowed = assignment?.attemptsAllowed ?: 0,
            canStart = canStart
        )
    }

    val uiState: StateFlow<StudentAssignmentDetailUiState> = combine(
        baseState,
        submitting,
        message,
        error
    ) { state, isSubmitting, msg, err ->
        state.copy(
            canStart = state.canStart && !isSubmitting,
            isSubmitting = isSubmitting,
            message = msg,
            error = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentAssignmentDetailUiState(assignmentId = assignmentId))

    fun startAssignment() {
        val current = uiState.value
        viewModelScope.launch {
            message.value = null
            error.value = null
            submitting.value = true
            runCatching {
                val userId = authRepository.authState.first().userId
                if (userId.isNullOrBlank()) error("Sign in to start the assignment")
                val uid = userId ?: return@runCatching
                val assignment = assignmentRepository.assignments.first().firstOrNull { it.id == assignmentId }
                    ?: error("Assignment not found")
                val submissions = assignmentRepository.submissionsForUser(uid).first()
                val submission = submissions.firstOrNull { it.assignmentId == assignmentId }
                val now = Clock.System.now()
                require(now in assignment.openAt..assignment.closeAt) { "Assignment is not open." }
                if (submission != null && submission.attempts >= assignment.attemptsAllowed) {
                    error("No attempts remaining")
                }
                submitAssignmentAttemptUseCase(assignmentId, uid, submission?.lastScore ?: 0)
            }.onSuccess {
                message.value = "Assignment started. Your submission has been recorded."
            }.onFailure { throwable ->
                error.value = throwable.message ?: "Unable to start assignment"
            }
            submitting.value = false
        }
    }

    private fun dueLabel(now: Instant, due: Instant): String {
        return if (due <= now) "Closed" else {
            val hours = (due - now).inWholeHours
            if (hours <= 48) "Due in ${hours}h" else "Due in ${(hours / 24)}d"
        }
    }
}
