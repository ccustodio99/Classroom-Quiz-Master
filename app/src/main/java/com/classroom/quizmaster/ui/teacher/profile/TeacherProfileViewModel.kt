package com.classroom.quizmaster.ui.teacher.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class TeacherProfileUiState(
    val displayNameInput: String = "",
    val email: String = "",
    val isSaving: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val savedMessage: String? = null,
    val lastSyncLabel: String = "Never"
)

@HiltViewModel
class TeacherProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val classroomRepository: ClassroomRepository,
    private val assignmentRepository: AssignmentRepository,
    private val quizRepository: QuizRepository,
    private val preferences: AppPreferencesDataSource
) : ViewModel() {

    private val editingName = MutableStateFlow("")
    private val saving = MutableStateFlow(false)
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val saved = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TeacherProfileUiState> = combine(
        authRepository.authState,
        editingName,
        saving,
        refreshing,
        error,
        saved,
        preferences.lastSuccessfulSyncEpoch
    ) { values: Array<Any?> ->
        val auth = values[0] as AuthState
        val nameInput = values[1] as String
        val isSaving = values[2] as Boolean
        val isRefreshing = values[3] as Boolean
        val errorMessage = values[4] as String?
        val savedMessage = values[5] as String?
        val lastSyncEpoch = values[6] as Long
        val currentName = auth.teacherProfile?.displayName ?: auth.displayName.orEmpty()
        val currentEmail = auth.teacherProfile?.email ?: auth.email.orEmpty()
        val effectiveName = if (nameInput.isNotEmpty()) nameInput else currentName
        TeacherProfileUiState(
            displayNameInput = effectiveName,
            email = currentEmail,
            isSaving = isSaving,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage,
            savedMessage = savedMessage,
            lastSyncLabel = formatLastSync(lastSyncEpoch)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeacherProfileUiState())

    fun updateDisplayName(value: String) {
        editingName.value = value.take(80)
    }

    fun save(onSaved: () -> Unit) {
        val targetName = uiState.value.displayNameInput.trim()
        if (targetName.isBlank()) {
            error.value = "Name cannot be empty"
            return
        }
        viewModelScope.launch {
            saving.value = true
            error.value = null
            saved.value = null
            runCatching { authRepository.updateDisplayName(targetName) }
                .onSuccess {
                    saved.value = "Profile updated"
                    editingName.value = targetName
                    onSaved()
                }
                .onFailure { err ->
                    error.value = err.message ?: "Unable to update profile"
                }
            saving.value = false
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            saved.value = null
            runCatching {
                classroomRepository.refresh()
                quizRepository.refresh()
                assignmentRepository.refreshAssignments()
            }.onSuccess {
                saved.value = "Data refreshed"
            }.onFailure { err ->
                error.value = err.message ?: "Unable to refresh data"
            }
            refreshing.value = false
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.logout() }
            onLoggedOut()
        }
    }

    private fun formatLastSync(epoch: Long): String =
        if (epoch <= 0L) "Never"
        else Instant.fromEpochMilliseconds(epoch).toString()
}
