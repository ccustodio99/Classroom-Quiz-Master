package com.classroom.quizmaster.data.lan

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class LanClient @Inject constructor(
    private val json: Json
) {

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _messages = MutableSharedFlow<WireMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<WireMessage> = _messages.asSharedFlow()

    private val _status = MutableStateFlow<LanClientStatus>(LanClientStatus.Disconnected)
    val status: StateFlow<LanClientStatus> = _status.asStateFlow()

    private val sessionMutex = Mutex()
    private var activeSession: DefaultClientWebSocketSession? = null
    private var connectJob: Job? = null
    private val reconnectAttempts = AtomicInteger(0)

    fun connect(endpoint: LanServiceDescriptor, uid: String) {
        reconnectAttempts.set(0)
        connectJob?.cancel()
        connectJob = scope.launch {
            while (isActive) {
                try {
                    _status.emit(LanClientStatus.Connecting(endpoint))
                    client.webSocket(
                        method = HttpMethod.Get,
                        request = { url("${endpoint.wsUri}?token=${endpoint.token}&uid=$uid") }
                    ) {
                        sessionMutex.withLock { activeSession = this }
                        reconnectAttempts.set(0)
                        _status.emit(LanClientStatus.Connected(endpoint))
                        receiveLoop()
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (t: Throwable) {
                    Timber.w(t, "LAN client connection failed")
                    _status.emit(LanClientStatus.Reconnecting(endpoint))
                    delay(calculateBackoff(reconnectAttempts.incrementAndGet()))
                } finally {
                    sessionMutex.withLock { activeSession = null }
                }
            }
        }
    }

    suspend fun sendAttempt(payload: WireMessage.AttemptSubmit): Boolean {
        val session = sessionMutex.withLock { activeSession }
        if (session == null) {
            Timber.w("Attempt send requested while not connected")
            return false
        }
        return runCatching {
            val frame = Frame.Text(json.encodeToString(payload))
            session.send(frame)
        }
            .onFailure { Timber.w(it, "Failed sending attempt ${payload.attemptId}") }
            .isSuccess
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        scope.launch {
            sessionMutex.withLock {
                runCatching { activeSession?.close(CloseReason(CloseReason.Codes.NORMAL, "client_disconnect")) }
                activeSession = null
            }
            _status.emit(LanClientStatus.Disconnected)
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveLoop() {
        try {
            for (frame in incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                runCatching { json.decodeFromString<WireMessage>(text) }
                    .onSuccess { message ->
                        if (message is WireMessage.SystemNotice) {
                            Timber.i("System notice from host: ${message.message}")
                        } else {
                            _messages.emit(message)
                        }
                    }
                    .onFailure { Timber.w(it, "Failed to decode LAN frame") }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Timber.w(t, "LAN client receive loop ended")
        }
    }

    private fun calculateBackoff(attempt: Int): Long {
        val clamped = attempt.coerceAtMost(6)
        return (INITIAL_RETRY_MS * (1 shl clamped)).coerceAtMost(MAX_RETRY_MS)
    }

    companion object {
        private const val INITIAL_RETRY_MS = 500L
        private const val MAX_RETRY_MS = 5_000L
    }
}
