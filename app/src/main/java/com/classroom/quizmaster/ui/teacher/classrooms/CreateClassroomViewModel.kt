package com.classroom.quizmaster.ui.teacher.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.util.JoinCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CreateClassroomUiState(
    val name: String = "",
    val grade: String = "",
    val subject: String = "",
    val joinCode: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving
}

@HiltViewModel
class CreateClassroomViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateClassroomUiState())
    val uiState: StateFlow<CreateClassroomUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(joinCode = JoinCodeGenerator.generate()) }
    }

    fun updateName(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun updateGrade(value: String) = _uiState.update { it.copy(grade = value) }
    fun updateSubject(value: String) = _uiState.update { it.copy(subject = value) }

    fun save(onSuccess: () -> Unit) {
        val current = _uiState.value
        if (!current.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val teacherId = authRepository.authState.first().userId
                    ?: error("No authenticated teacher available")
                val now = Clock.System.now()
                val classroom = Classroom(
                    id = "",
                    teacherId = teacherId,
                    name = current.name.trim(),
                    grade = current.grade.trim(),
                    subject = current.subject.trim(),
                    joinCode = current.joinCode,
                    createdAt = now,
                    updatedAt = now
                )
                classroomRepository.upsertClassroom(classroom)
            }.onSuccess {
                _uiState.value = CreateClassroomUiState(success = true)
                onSuccess()
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Unable to save classroom") }
            }
        }
    }
}
