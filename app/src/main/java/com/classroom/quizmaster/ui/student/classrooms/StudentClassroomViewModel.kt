package com.classroom.quizmaster.ui.student.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentClassroomUiState(
    val classrooms: List<ClassroomSummaryUi> = emptyList()
)

data class ClassroomSummaryUi(
    val id: String,
    val name: String,
    val teacherName: String,
    val joinCode: String
)

@HiltViewModel
class StudentClassroomViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    init {
        viewModelScope.launch { classroomRepository.refresh() }
    }

    val uiState: StateFlow<StudentClassroomUiState> =
        combine(
            classroomRepository.classrooms,
            authRepository.authState
        ) { classrooms, _ ->
            val teacherNames = classrooms
                .map { it.teacherId }
                .distinct()
                .associateWith { teacherId ->
                    runCatching { authRepository.getTeacher(teacherId).first()?.displayName }
                        .getOrNull()
                        .orEmpty()
                }
            StudentClassroomUiState(
                classrooms = classrooms.filterNot { it.isArchived }.map { classroom ->
                    ClassroomSummaryUi(
                        id = classroom.id,
                        name = classroom.name,
                        teacherName = teacherNames[classroom.teacherId]
                            .orEmpty()
                            .ifBlank { "Teacher" },
                        joinCode = classroom.joinCode
                    )
                }
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StudentClassroomUiState()
            )
}
