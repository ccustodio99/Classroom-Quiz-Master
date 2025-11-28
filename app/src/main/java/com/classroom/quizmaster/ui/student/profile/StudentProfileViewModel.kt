package com.classroom.quizmaster.ui.student.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Assignment
import kotlinx.datetime.Instant

data class StudentProfileUiState(
    val fullName: String = "Student",
    val email: String? = null,
    val classroomsCount: Int = 0,
    val completedAssignments: Int = 0,
    val nameInput: String = "",
    val saving: Boolean = false,
    val refreshing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastSyncLabel: String = "Never"
)

@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val classroomRepository: ClassroomRepository,
    private val assignmentRepository: AssignmentRepository,
    private val preferences: AppPreferencesDataSource
) : ViewModel() {

    private val nameInput = MutableStateFlow("")
    private val saving = MutableStateFlow(false)
    private val refreshing = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val baseState = combine(
        authRepository.authState,
        classroomRepository.classrooms,
        assignmentRepository.assignments,
        nameInput,
        saving,
        refreshing
    ) { values: Array<Any?> ->
        val auth = values[0] as AuthState
        val classrooms = values[1] as List<Classroom>
        val assignments = values[2] as List<Assignment>
        val nameInputValue = values[3] as String
        val savingValue = values[4] as Boolean
        val refreshingValue = values[5] as Boolean
        val now = Clock.System.now()
        StudentProfileUiState(
            fullName = auth.displayName ?: "Student",
            email = auth.email,
            classroomsCount = classrooms.count { !it.isArchived },
            completedAssignments = assignments.count { it.closeAt < now || it.isArchived },
            nameInput = if (nameInputValue.isBlank()) auth.displayName.orEmpty() else nameInputValue,
            saving = savingValue,
            refreshing = refreshingValue
        )
    }

    val uiState: StateFlow<StudentProfileUiState> = combine(
        baseState,
        statusMessage,
        errorMessage,
        preferences.lastSuccessfulSyncEpoch
    ) { state, status, error, lastSync ->
        state.copy(
            statusMessage = status,
            errorMessage = error,
            lastSyncLabel = formatLastSync(lastSync)
        )
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

    fun refreshData() {
        viewModelScope.launch {
            refreshing.value = true
            errorMessage.value = null
            statusMessage.value = null
            runCatching {
                classroomRepository.refresh()
                assignmentRepository.refreshAssignments()
            }.onSuccess {
                statusMessage.value = "Data refreshed"
            }.onFailure { err ->
                errorMessage.value = err.message ?: "Unable to refresh data"
            }
            refreshing.value = false
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

    private fun formatLastSync(epoch: Long): String =
        if (epoch <= 0L) "Never"
        else Instant.fromEpochMilliseconds(epoch).toString()
}
