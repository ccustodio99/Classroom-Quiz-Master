package com.classroom.quizmaster.ui.teacher.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.ui.student.classrooms.ClassroomSummaryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherClassroomsViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    val uiState: StateFlow<TeacherClassroomUiState> =
        combine(
            classroomRepository.classrooms,
            classroomRepository.archivedClassrooms
        ) { active, archived ->
            TeacherClassroomUiState(
                activeClassrooms = active.map {
                    ClassroomSummaryUi(
                        id = it.id,
                        name = it.name,
                        teacherName = "",
                        joinCode = it.joinCode,
                        subject = it.subject,
                        grade = it.grade,
                        activeAssignments = 0,
                        studentCount = it.students.size
                    )
                },
                archivedClassrooms = archived.map {
                    ClassroomSummaryUi(
                        id = it.id,
                        name = it.name,
                        teacherName = "",
                        joinCode = it.joinCode,
                        subject = it.subject,
                        grade = it.grade,
                        activeAssignments = 0,
                        studentCount = it.students.size
                    )
                }
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TeacherClassroomUiState()
            )

    fun archive(classroomId: String) {
        viewModelScope.launch {
            classroomRepository.archiveClassroom(classroomId)
        }
    }
}

data class TeacherClassroomUiState(
    val activeClassrooms: List<ClassroomSummaryUi> = emptyList(),
    val archivedClassrooms: List<ClassroomSummaryUi> = emptyList(),
    val emptyMessage: String = ""
)
