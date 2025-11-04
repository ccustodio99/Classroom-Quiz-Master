package com.example.lms.feature.live.ui

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lms.core.model.LeaderboardEntry
import com.example.lms.core.network.live.LanDiscoveryManager
import com.example.lms.core.network.live.WebRtcPeerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

enum class LiveMode { HOST, PARTICIPANT }

data class LiveQuestionUi(
    val title: String,
    val prompt: String,
    val type: String,
    val options: List<String> = emptyList(),
    val sliderRange: IntRange? = null,
)

data class LiveUiState(
    val mode: LiveMode,
    val sessionCode: String,
    val connectionStatus: String,
    val currentQuestionIndex: Int,
    val questions: List<LiveQuestionUi>,
    val leaderboard: List<LeaderboardEntry>,
    val availablePeers: List<String> = emptyList(),
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val lanDiscoveryManager: LanDiscoveryManager,
    private val webRtcPeerSession: WebRtcPeerSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var hostingStarted = false

    init {
        observeLanState()
        observeConnectionState()
        lanDiscoveryManager.startDiscovery()
        if (_uiState.value.mode == LiveMode.HOST) {
            startHosting()
        }
    }

    private fun createInitialState(): LiveUiState {
        val questions = listOf(
            LiveQuestionUi(
                title = "Question 1",
                prompt = "Which structure captures light energy during photosynthesis?",
                type = "MCQ",
                options = listOf("Mitochondria", "Chloroplast", "Nucleus", "Ribosome"),
            ),
            LiveQuestionUi(
                title = "Question 2",
                prompt = "True or false: The Calvin cycle occurs in the thylakoid lumen.",
                type = "TF",
                options = listOf("True", "False"),
            ),
            LiveQuestionUi(
                title = "Question 3",
                prompt = "Drag the phases of photosynthesis into order.",
                type = "Puzzle",
                options = listOf("Light capture", "Electron transport", "ATP synthase", "Carbon fixation"),
            ),
            LiveQuestionUi(
                title = "Question 4",
                prompt = "Estimate the pH change during the light reactions.",
                type = "Slider",
                sliderRange = 0..14,
            ),
        )
        val leaderboard = listOf(
            LeaderboardEntry("alex", "Alex", 920.0, 3, 850),
            LeaderboardEntry("jordan", "Jordan", 880.0, 2, 1020),
            LeaderboardEntry("priya", "Priya", 870.0, 4, 910),
        )
        return LiveUiState(
            mode = LiveMode.HOST,
            sessionCode = generateSessionCode(),
            connectionStatus = "Initializing session",
            currentQuestionIndex = 0,
            questions = questions,
            leaderboard = leaderboard,
        )
    }

    private fun observeLanState() {
        viewModelScope.launch {
            lanDiscoveryManager.services.collect { services ->
                val peers = services.map { service ->
                    val device = service.attributes[ATTR_DEVICE]?.takeIf { it.isNotBlank() }
                    val code = service.attributes[ATTR_CODE]?.takeIf { it.isNotBlank() }
                    when {
                        device != null && code != null -> "$device • $code"
                        device != null -> device
                        code != null -> code
                        else -> service.serviceName
                    }
                }
                _uiState.update { it.copy(availablePeers = peers) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            combine(
                lanDiscoveryManager.isAdvertising,
                lanDiscoveryManager.isDiscovering,
                webRtcPeerSession.connectionState,
            ) { advertising, discovering, connection ->
                when {
                    connection == PeerConnection.PeerConnectionState.CONNECTED -> "Connected via LAN DataChannel"
                    advertising -> "Hosting on LAN (${_uiState.value.sessionCode})"
                    discovering -> "Discovering peers on LAN…"
                    else -> "Idle"
                }
            }.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }
    }

    private fun startHosting() {
        if (hostingStarted) return
        hostingStarted = true
        lanDiscoveryManager.registerService(
            DEFAULT_LIVE_PORT,
            attributes = mapOf(
                ATTR_MODE to LiveMode.HOST.name.lowercase(),
                ATTR_CODE to _uiState.value.sessionCode,
                ATTR_DEVICE to Build.MODEL,
            ),
        )
        webRtcPeerSession.createPeerConnection(WebRtcPeerSession.Mode.HOST)
    }

    override fun onCleared() {
        lanDiscoveryManager.shutdown()
        webRtcPeerSession.close()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_LIVE_PORT = 59000
        private const val ATTR_MODE = "mode"
        private const val ATTR_CODE = "code"
        private const val ATTR_DEVICE = "device"

        private fun generateSessionCode(): String = buildString {
            repeat(3) {
                if (isNotEmpty()) append(' ')
                append(Random.nextInt(100, 999))
            }
        }
    }
}
