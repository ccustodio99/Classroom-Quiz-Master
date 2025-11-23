package com.classroom.quizmaster.ui.student.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.SessionStatus
import com.classroom.quizmaster.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CurrentSessionUi(
    val sessionId: String = "",
    val joinCode: String = "",
    val status: String = "",
    val classroomId: String = ""
)

@HiltViewModel
class StudentJoinStatusViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {

    val current: StateFlow<CurrentSessionUi?> = sessionRepository.session
        .map { session ->
            session?.takeIf { it.status != SessionStatus.ENDED }?.let {
                CurrentSessionUi(
                    sessionId = it.id,
                    joinCode = it.joinCode,
                    status = it.status.name.lowercase(),
                    classroomId = it.classroomId
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
