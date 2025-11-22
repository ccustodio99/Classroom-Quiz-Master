package com.classroom.quizmaster.ui.student.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherSearchUiState(
    val teachers: List<Teacher> = emptyList(),
    val selectedTeacher: Teacher? = null,
    val classrooms: List<Classroom> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val joinMessage: String? = null,
    val isJoining: Boolean = false
)

@HiltViewModel
class TeacherSearchViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherSearchUiState())
    val uiState: StateFlow<TeacherSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.length < 3) {
            _uiState.update { it.copy(teachers = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                classroomRepository.searchTeachers(query)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(isLoading = false, teachers = it)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onTeacherSelected(teacher: Teacher) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedTeacher = teacher,
                    joinMessage = null,
                    errorMessage = null
                )
            }
            runCatching {
                classroomRepository.getClassroomsForTeacher(teacher.id)
            }.onSuccess { classrooms ->
                _uiState.update { state ->
                    state.copy(isLoading = false, classrooms = classrooms)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun joinClassroom(classroomId: String, teacherId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null, joinMessage = null) }
            runCatching {
                classroomRepository.createJoinRequest(classroomId, teacherId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isJoining = false,
                        joinMessage = "Request sent. Wait for the teacher to approve.",
                        errorMessage = null
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isJoining = false, errorMessage = e.message) }
            }
        }
    }
}
