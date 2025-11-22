package com.classroom.quizmaster.ui.student.classrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StudentClassroomViewModel @Inject constructor(
    classroomRepository: ClassroomRepository
) : ViewModel() {

    val uiState: StateFlow<StudentClassroomUiState> =
        classroomRepository.classrooms
            .map { classrooms ->
                StudentClassroomUiState(
                    classrooms = classrooms.filter { !it.isArchived }.map {
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
                initialValue = StudentClassroomUiState()
            )
}
