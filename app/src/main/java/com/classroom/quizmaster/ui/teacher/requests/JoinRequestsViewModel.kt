package com.classroom.quizmaster.ui.teacher.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.JoinRequestStatus
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinRequestsUiState(
    val joinRequests: List<JoinRequestUi> = emptyList()
)

data class JoinRequestUi(
    val id: String,
    val studentName: String,
    val classroomName: String,
    val status: JoinRequestStatus
)

@HiltViewModel
class JoinRequestsViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    init {
        viewModelScope.launch { classroomRepository.refresh() }
    }

    val uiState: StateFlow<JoinRequestsUiState> =
        combine(
            classroomRepository.joinRequests,
            classroomRepository.classrooms
        ) { requests, classrooms ->
            val classroomsById = classrooms.associateBy { it.id }
            val detailed = requests.map { request ->
                val studentName = classroomRepository.getStudent(request.studentId)?.displayName
                    ?: request.studentId
                val classroomName = classroomsById[request.classroomId]?.name ?: request.classroomId
                JoinRequestUi(
                    id = request.id,
                    studentName = studentName,
                    classroomName = classroomName,
                    status = request.status
                )
            }
            JoinRequestsUiState(joinRequests = detailed)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = JoinRequestsUiState()
            )

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            classroomRepository.approveJoinRequest(requestId)
        }
    }

    fun denyRequest(requestId: String) {
        viewModelScope.launch {
            classroomRepository.denyJoinRequest(requestId)
        }
    }
}
