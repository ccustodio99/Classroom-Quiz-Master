package com.classroom.quizmaster.ui.student.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class StudentProfileUiState(
    val fullName: String = "Student",
    val email: String? = null,
    val classroomsCount: Int = 0,
    val completedAssignments: Int = 0,
    val nameInput: String = "",
    val saving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val classroomRepository: ClassroomRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val nameInput = MutableStateFlow("")
    private val saving = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val baseState = combine(
        authRepository.authState,
        classroomRepository.classrooms,
        assignmentRepository.assignments,
        nameInput,
        saving
    ) { auth, classrooms, assignments, nameInputValue, savingValue ->
        val now = Clock.System.now()
        StudentProfileUiState(
            fullName = auth.displayName ?: "Student",
            email = auth.email,
            classroomsCount = classrooms.count { !it.isArchived },
            completedAssignments = assignments.count { it.closeAt < now || it.isArchived },
            nameInput = if (nameInputValue.isBlank()) auth.displayName.orEmpty() else nameInputValue,
            saving = savingValue
        )
    }

    val uiState: StateFlow<StudentProfileUiState> = combine(
        baseState,
        statusMessage,
        errorMessage
    ) { state, status, error ->
        state.copy(statusMessage = status, errorMessage = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentProfileUiState())

    fun updateNameInput(value: String) {
        nameInput.value = value
        errorMessage.value = null
        statusMessage.value = null
    }

    fun saveName() {
        val desired = nameInput.value.trim()
        if (desired.isBlank()) {
            errorMessage.value = "Enter your full name"
            return
        }
        viewModelScope.launch {
            saving.value = true
            errorMessage.value = null
            statusMessage.value = null
            runCatching { authRepository.updateDisplayName(desired) }
                .onSuccess {
                    saving.value = false
                    statusMessage.value = "Name updated"
                    nameInput.value = desired
                }
                .onFailure { error ->
                    saving.value = false
                    errorMessage.value = error.message ?: "Unable to update name"
                }
        }
    }

    fun sendPasswordReset() {
        val email = uiState.value.email ?: return
        viewModelScope.launch {
            runCatching { authRepository.sendPasswordReset(email) }
                .onSuccess {
                    statusMessage.value = "Password reset email sent to $email"
                }
                .onFailure { error ->
                    errorMessage.value = error.message ?: "Unable to send reset email"
                }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
