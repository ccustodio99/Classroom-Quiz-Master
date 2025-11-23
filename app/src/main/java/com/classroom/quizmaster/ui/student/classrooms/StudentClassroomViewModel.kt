package com.classroom.quizmaster.ui.student.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
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
    val joinCode: String,
    val subject: String,
    val grade: String,
    val activeAssignments: Int
)

@HiltViewModel
class StudentClassroomViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    private val authRepository: AuthRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    init {
        viewModelScope.launch { classroomRepository.refresh() }
    }

    val uiState: StateFlow<StudentClassroomUiState> =
        combine(
            classroomRepository.classrooms,
            authRepository.authState,
            assignmentRepository.assignments
        ) { classrooms, _, assignments ->
            val teacherNames = classrooms
                .map { it.teacherId }
                .distinct()
                .associateWith { teacherId ->
                    runCatching { authRepository.getTeacher(teacherId).first()?.displayName }
                        .getOrNull()
                        .orEmpty()
                }
            val now = kotlinx.datetime.Clock.System.now()
            val activeAssignmentsByClassroom = assignments
                .filter { !it.isArchived && it.closeAt > now }
                .groupBy { it.classroomId }
                .mapValues { (_, list) -> list.size }

            StudentClassroomUiState(
                classrooms = classrooms.filterNot { it.isArchived }.map { classroom ->
                    ClassroomSummaryUi(
                        id = classroom.id,
                        name = classroom.name,
                        teacherName = teacherNames[classroom.teacherId]
                            .orEmpty()
                            .ifBlank { "Teacher" },
                        joinCode = classroom.joinCode,
                        subject = classroom.subject,
                        grade = classroom.grade,
                        activeAssignments = activeAssignmentsByClassroom[classroom.id] ?: 0
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
