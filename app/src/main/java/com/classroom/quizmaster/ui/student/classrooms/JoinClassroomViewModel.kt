package com.classroom.quizmaster.ui.student.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JoinClassroomUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class JoinClassroomViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinClassroomUiState())
    val uiState: StateFlow<JoinClassroomUiState> = _uiState.asStateFlow()

    fun joinClassroom(joinCode: String, onJoinSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, statusMessage = null, success = false) }
            runCatching {
                classroomRepository.createJoinRequest(joinCode.trim())
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        success = true,
                        statusMessage = "Request sent. Wait for teacher approval."
                    )
                }
                onJoinSuccess()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}