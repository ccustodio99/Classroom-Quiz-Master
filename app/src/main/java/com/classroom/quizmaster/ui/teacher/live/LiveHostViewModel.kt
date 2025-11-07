package com.classroom.quizmaster.ui.teacher.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.LanMeta
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveHostUiState(
    val session: Session? = null,
    val participants: List<Participant> = emptyList(),
    val lanMeta: LanMeta? = null
)

@HiltViewModel
class LiveHostViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val uiState: StateFlow<LiveHostUiState> = combine(
        sessionRepository.session,
        sessionRepository.participants,
        sessionRepository.lanMeta
    ) { session, participants, lanMeta ->
        LiveHostUiState(session, participants, lanMeta)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveHostUiState())

    fun startSession(quizId: String, classroomId: String, nickname: String) {
        viewModelScope.launch {
            sessionRepository.startLanSession(quizId, classroomId, nickname)
        }
    }

    fun endSession() {
        viewModelScope.launch { sessionRepository.endSession() }
    }

    fun kick(uid: String) {
        viewModelScope.launch { sessionRepository.kickParticipant(uid) }
    }
}
