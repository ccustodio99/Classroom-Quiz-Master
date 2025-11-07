package com.classroom.quizmaster.data.lan

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
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

    private val _messages = MutableSharedFlow<WireMessage>(extraBufferCapacity = 32)
    val messages: SharedFlow<WireMessage> = _messages.asSharedFlow()
    private var connectJob: Job? = null

    fun connect(endpoint: LanServiceDescriptor, uid: String) {
        connectJob?.cancel()
        connectJob = scope.launch {
            var attempt = 0
            while (isActive) {
                try {
                    client.webSocket(
                        method = HttpMethod.Get,
                        request = {
                            url("${endpoint.wsUri}?token=${endpoint.token}&uid=$uid")
                        }
                    ) {
                        attempt = 0
                        for (frame in incoming) {
                            val text = (frame as? Frame.Text)?.readText() ?: continue
                            runCatching {
                                json.decodeFromString(WireMessage.serializer(), text)
                            }.onSuccess { decoded ->
                                _messages.emit(decoded)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Timber.e(t, "LAN client connection failed")
                    attempt++
                    val delayMs = (500L * (1 shl attempt.coerceAtMost(4))).coerceAtMost(5_000L)
                    delay(delayMs)
                }
            }
        }
    }

    suspend fun sendAttempt(endpoint: LanServiceDescriptor, payload: WireMessage.AttemptSubmit) {
        client.webSocket(
            method = HttpMethod.Get,
            request = { url("${endpoint.wsUri}?token=${endpoint.token}&uid=${payload.uid}") }
        ) {
            send(Frame.Text(json.encodeToString(WireMessage.serializer(), payload)))
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
    }
}
