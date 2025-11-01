package com.classroom.quizmaster.ui.feature.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.agents.AnswerPayload
import com.classroom.quizmaster.lan.LanDiscoveryClient
import com.classroom.quizmaster.lan.LanParticipantClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JoinSessionViewModel : ViewModel() {

    private val discoveryClient = LanDiscoveryClient()
    private val participantClient = LanParticipantClient()
    private var studentId: String? = null
    private var connectedSession: String? = null
    private var snapshotJob: Job? = null

    private val _uiState = MutableStateFlow(JoinSessionUiState())
    val uiState: StateFlow<JoinSessionUiState> = _uiState.asStateFlow()

    fun onNicknameChanged(value: String) {
        _uiState.update { it.copy(nickname = value) }
    }

    fun onSessionCodeChanged(value: String) {
        _uiState.update { it.copy(sessionCode = value.uppercase()) }
    }

    fun onHostAddressChanged(value: String) {
        _uiState.update { it.copy(manualHost = value) }
    }

    fun scanLocalNetwork() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = JoinStatus.Scanning, availableSessions = emptyList()) }
            val announcements = discoveryClient.discover()
            _uiState.update {
                it.copy(
                    status = JoinStatus.Idle,
                    availableSessions = announcements.map { announcement ->
                        DiscoveredSession(
                            sessionId = announcement.sessionId,
                            host = announcement.host,
                            participants = announcement.participants,
                            moduleId = announcement.moduleId
                        )
                    }
                )
            }
        }
    }

    fun connectToSession(host: String, sessionCode: String) {
        val nickname = _uiState.value.nickname.ifBlank { "Guest" }
        viewModelScope.launch {
            _uiState.update { it.copy(status = JoinStatus.Connecting, hostAddress = host) }
            val ack = participantClient.join(host, sessionCode, nickname)
            if (!ack.accepted) {
                _uiState.update {
                    it.copy(
                        status = JoinStatus.Error(ack.reason ?: "Unable to join"),
                        hostAddress = null,
                        connected = false
                    )
                }
                return@launch
            }
            studentId = ack.studentId
            connectedSession = sessionCode
            _uiState.update {
                it.copy(
                    status = JoinStatus.Connected,
                    connected = true,
                    hostAddress = host,
                    manualHost = host,
                    sessionCode = sessionCode,
                    nickname = ack.displayName ?: nickname,
                    availableSessions = emptyList()
                )
            }
            listenForUpdates()
        }
    }

    fun joinDiscoveredSession(session: DiscoveredSession) {
        _uiState.update {
            it.copy(
                sessionCode = session.sessionId,
                manualHost = session.host
            )
        }
        connectToSession(session.host, session.sessionId)
    }

    fun submitChoice(choice: String) {
        val sessionId = connectedSession ?: return
        val student = studentId ?: return
        val currentSnapshot = _uiState.value.lastSnapshot
        val itemId = currentSnapshot?.activeItemId ?: ""
        val payload = AnswerPayload(itemId = itemId, answer = choice, studentId = student)
        participantClient.sendAnswer(sessionId, student, payload)
    }

    fun submitTypedAnswer(answer: String) {
        if (answer.isBlank()) return
        submitChoice(answer)
        _uiState.update { it.copy(pendingTypedAnswer = "") }
    }

    fun onTypedAnswerChanged(value: String) {
        _uiState.update { it.copy(pendingTypedAnswer = value) }
    }

    fun requestRefresh() {
        val sessionId = connectedSession ?: return
        participantClient.requestSnapshot(sessionId)
    }

    fun disconnect() {
        studentId = null
        connectedSession = null
        participantClient.disconnect()
        snapshotJob?.cancel()
        snapshotJob = null
        _uiState.update {
            it.copy(
                status = JoinStatus.Idle,
                connected = false,
                hostAddress = null,
                lastSnapshot = null,
                activePrompt = null,
                activeObjective = null,
                answerAck = null
            )
        }
    }

    private fun listenForUpdates() {
        snapshotJob?.cancel()
        snapshotJob = viewModelScope.launch {
            participantClient.snapshots.collect { snapshotMessage ->
                val snapshot = snapshotMessage?.snapshot
                if (snapshot != null) {
                    _uiState.update {
                        it.copy(
                            lastSnapshot = snapshot,
                            activePrompt = snapshot.activePrompt,
                            activeObjective = snapshot.activeObjective,
                            answerAck = null
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            participantClient.answerAcks.collect { ack ->
                _uiState.update {
                    it.copy(
                        answerAck = if (ack.accepted) "Answer received" else ack.reason ?: "Answer rejected"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        participantClient.shutdown()
    }
}

data class JoinSessionUiState(
    val sessionCode: String = "",
    val nickname: String = "",
    val hostAddress: String? = null,
    val manualHost: String = "",
    val availableSessions: List<DiscoveredSession> = emptyList(),
    val status: JoinStatus = JoinStatus.Idle,
    val connected: Boolean = false,
    val activePrompt: String? = null,
    val activeObjective: String? = null,
    val lastSnapshot: com.classroom.quizmaster.agents.LiveSnapshot? = null,
    val answerAck: String? = null,
    val pendingTypedAnswer: String = ""
)

sealed class JoinStatus {
    data object Idle : JoinStatus()
    data object Scanning : JoinStatus()
    data object Connecting : JoinStatus()
    data object Connected : JoinStatus()
    data class Error(val message: String) : JoinStatus()
}

data class DiscoveredSession(
    val sessionId: String,
    val host: String,
    val participants: Int,
    val moduleId: String
)
