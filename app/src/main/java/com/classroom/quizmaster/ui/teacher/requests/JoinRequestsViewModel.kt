package com.classroom.quizmaster.ui.teacher.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.JoinRequest
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinRequestsUiState(
    val joinRequests: List<JoinRequest> = emptyList()
)

@HiltViewModel
class JoinRequestsViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    val uiState: StateFlow<JoinRequestsUiState> =
        classroomRepository.joinRequests
            .map { requests ->
                JoinRequestsUiState(joinRequests = requests)
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
