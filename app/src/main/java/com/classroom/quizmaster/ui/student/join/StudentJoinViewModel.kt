package com.classroom.quizmaster.ui.student.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.data.lan.LanDiscoveryEvent
import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.data.lan.NearbyFallbackManager
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.util.NicknamePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class StudentJoinUiState(
    val services: List<LanServiceDescriptor> = emptyList(),
    val isDiscovering: Boolean = false,
    val error: String? = null,
    val timedOut: Boolean = false,
    val nickname: String = "",
    val manualUri: String = "",
    val isJoining: Boolean = false,
    val nicknameError: String? = null
)

@HiltViewModel
class StudentJoinViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val nearbyFallbackManager: NearbyFallbackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentJoinUiState(nickname = "Student"))
    val uiState: StateFlow<StudentJoinUiState> = _uiState
    private var discoveryJob: Job? = null

    fun discoverLanHosts() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                services = emptyList(),
                isDiscovering = true,
                timedOut = false,
                error = null
            )
            sessionRepository.discoverHosts().collectLatest { event ->
                when (event) {
                    is LanDiscoveryEvent.ServiceFound -> {
                        val updated = (_uiState.value.services + event.descriptor)
                            .distinctBy { it.serviceName }
                        _uiState.value = _uiState.value.copy(
                            services = updated,
                            isDiscovering = false
                        )
                    }

                    is LanDiscoveryEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = event.message,
                            isDiscovering = false
                        )
                    }

                    LanDiscoveryEvent.Timeout -> {
                        _uiState.value = _uiState.value.copy(
                            timedOut = true,
                            isDiscovering = false
                        )
                        launchFallbackDiscovery()
                    }
                }
            }
        }
    }

    fun retryDiscovery() = discoverLanHosts()

    fun updateNickname(value: String) {
        _uiState.value = _uiState.value.copy(
            nickname = value,
            nicknameError = NicknamePolicy.validationError(value)
        )
    }

    fun updateManualUri(value: String) {
        _uiState.value = _uiState.value.copy(manualUri = value)
    }

    fun join(service: LanServiceDescriptor, onSuccess: () -> Unit = {}) {
        val current = _uiState.value
        val violation = NicknamePolicy.validationError(current.nickname)
        if (violation != null) {
            _uiState.value = current.copy(nicknameError = violation)
            return
        }
        val sanitized = NicknamePolicy.sanitize(
            current.nickname.ifBlank { "Student" },
            service.joinCode + service.timestamp.toString()
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isJoining = true, error = null, nicknameError = null)
            sessionRepository.joinLanHost(service, sanitized)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isJoining = false)
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isJoining = false,
                        error = throwable.message ?: "Unable to join"
                    )
                }
        }
    }

    fun joinFromUri(onSuccess: () -> Unit = {}) {
        val uri = _uiState.value.manualUri
        if (uri.isBlank()) return
        runCatching { parseDescriptor(uri) }
            .onSuccess { join(it, onSuccess) }
            .onFailure {
                _uiState.value = _uiState.value.copy(error = "Invalid QR/URI payload")
            }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, nicknameError = null)
    }

    private fun parseDescriptor(uriString: String): LanServiceDescriptor {
        val parsed = URI(uriString)
        val queryToken = parsed.query.orEmpty()
            .split("&")
            .mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .firstOrNull { it.first == "token" }
            ?.second
            ?: ""
        return LanServiceDescriptor(
            serviceName = "manual-${parsed.host}",
            host = parsed.host,
            port = if (parsed.port == -1) 80 else parsed.port,
            token = queryToken,
            joinCode = "",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun launchFallbackDiscovery() {
        viewModelScope.launch {
            nearbyFallbackManager.discover().collectLatest { event ->
                when (event) {
                    is LanDiscoveryEvent.ServiceFound -> {
                        val updated = (_uiState.value.services + event.descriptor)
                            .distinctBy { it.serviceName }
                        _uiState.value = _uiState.value.copy(services = updated)
                    }

                    is LanDiscoveryEvent.Error -> {
                        _uiState.value = _uiState.value.copy(error = event.message)
                    }

                    LanDiscoveryEvent.Timeout -> Unit
                }
            }
        }
    }
}
