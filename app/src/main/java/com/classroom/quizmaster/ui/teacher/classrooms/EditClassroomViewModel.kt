package com.classroom.quizmaster.ui.teacher.classrooms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@HiltViewModel
class EditClassroomViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classroomId: String = savedStateHandle[CLASSROOM_ID_KEY]
        ?: throw IllegalArgumentException("classroomId is required")

    private val _uiState = MutableStateFlow(EditClassroomUiState(classroomId = classroomId))
    val uiState: StateFlow<EditClassroomUiState> = _uiState.asStateFlow()

    private var original: Classroom? = null

    init {
        viewModelScope.launch { loadClassroom() }
    }

    private suspend fun loadClassroom() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { classroomRepository.getClassroom(classroomId) }
            .onSuccess { classroom ->
                if (classroom == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Classroom unavailable"
                        )
                    }
                } else {
                    original = classroom
                    _uiState.update {
                        it.copy(
                            name = classroom.name,
                            grade = classroom.grade,
                            subject = classroom.subject,
                            isLoading = false
                        )
                    }
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load classroom"
                    )
                }
            }
    }

    fun updateName(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun updateGrade(value: String) = _uiState.update { it.copy(grade = value) }
    fun updateSubject(value: String) = _uiState.update { it.copy(subject = value) }

    fun save() {
        val state = _uiState.value
        val base = original ?: return
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val updated = base.copy(
                    name = state.name.trim(),
                    grade = state.grade.trim(),
                    subject = state.subject.trim(),
                    updatedAt = Clock.System.now()
                )
                classroomRepository.upsertClassroom(updated)
                updated
            }
                .onSuccess { updated ->
                    original = updated
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Unable to save classroom"
                        )
                    }
                }
        }
    }

    fun archive() {
        if (_uiState.value.isArchiving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isArchiving = true, errorMessage = null) }
            runCatching { classroomRepository.archiveClassroom(classroomId) }
                .onSuccess {
                    _uiState.update { it.copy(isArchiving = false, archiveSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isArchiving = false,
                            errorMessage = error.message ?: "Unable to archive classroom"
                        )
                    }
                }
        }
    }

    companion object {
        const val CLASSROOM_ID_KEY = "classroomId"
    }
}

data class EditClassroomUiState(
    val classroomId: String = "",
    val name: String = "",
    val grade: String = "",
    val subject: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isArchiving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val archiveSuccess: Boolean = false
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving && !isArchiving && !isLoading
}
