package com.classroom.quizmaster.ui.student.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class StudentProfileUiState(
    val fullName: String = "Student",
    val email: String? = null,
    val classroomsCount: Int = 0,
    val completedAssignments: Int = 0
)

@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val classroomRepository: ClassroomRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    val uiState: StateFlow<StudentProfileUiState> = combine(
        authRepository.authState,
        classroomRepository.classrooms,
        assignmentRepository.assignments
    ) { auth, classrooms, assignments ->
        val now = Clock.System.now()
        StudentProfileUiState(
            fullName = auth.displayName ?: "Student",
            email = auth.email,
            classroomsCount = classrooms.count { !it.isArchived },
            completedAssignments = assignments.count { it.closeAt < now || it.isArchived }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentProfileUiState())

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
