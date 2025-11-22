package com.classroom.quizmaster.ui.student.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherClassroomsViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    val uiState: StateFlow<TeacherClassroomUiState> =
        classroomRepository.classrooms
            .map { classrooms ->
                TeacherClassroomUiState(
                    classrooms = classrooms.map {
                        ClassroomSummaryUi(
                            id = it.id,
                            name = it.name,
                            teacherName = "", // TODO: Get teacher name
                            joinCode = it.joinCode
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
    val classrooms: List<ClassroomSummaryUi> = emptyList(),
    val emptyMessage: String = ""
)
