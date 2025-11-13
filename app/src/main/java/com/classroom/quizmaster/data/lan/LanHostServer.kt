package com.classroom.quizmaster.data.lan

import com.classroom.quizmaster.BuildConfig
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import kotlin.jvm.Volatile

@Singleton
class LanHostServer @Inject constructor(
    private val json: Json
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: ApplicationEngine? = null
    private val clients = ConcurrentHashMap<String, Channel<WireMessage>>()
    private val processedAttempts = ConcurrentHashMap<String, Long>()
    private val _attempts = MutableSharedFlow<WireMessage.AttemptSubmit>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val attemptSubmissions: SharedFlow<WireMessage.AttemptSubmit> = _attempts.asSharedFlow()

    @Volatile
    private var _activePort: Int? = null
    val activePort: Int? get() = _activePort

    private val cleanupJob: Job = scope.launch {
        while (isActive) {
            delay(PRUNE_INTERVAL_MS)
            pruneAttempts()
        }
    }

    fun start(token: String, requestedPort: Int = BuildConfig.LAN_DEFAULT_PORT): Int {
        stop()
        val engine = embeddedServer(
            factory = CIO,
            port = requestedPort,
            host = BuildConfig.LAN_DEFAULT_HOST
        ) {
            configure(token)
        }
        engine.start()
        server = engine
        val boundPort = engine.environment.connectors.firstOrNull()?.port ?: requestedPort
        _activePort = boundPort
        Timber.i("LAN host server started on port %d", boundPort)
        return boundPort
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 500, timeoutMillis = 1_000)
        server = null
        _activePort = null
        clients.values.forEach(Channel<WireMessage>::close)
        clients.clear()
        processedAttempts.clear()
    }

    suspend fun broadcast(message: WireMessage) {
        clients.entries.removeIf { (_, channel) ->
            val result = channel.trySend(message)
            if (!result.isSuccess) {
                channel.close()
            }
            !result.isSuccess
        }
    }

    fun kick(uid: String) {
        clients.remove(uid)?.let { channel ->
            scope.launch {
                channel.trySend(WireMessage.SystemNotice("removed_by_host"))
                channel.close()
            }
        }
    }

    private fun Application.configure(token: String) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
            pingPeriodMillis = 15_000
            timeoutMillis = 30_000
            maxFrameSize = 64 * 1024L
        }
        install(CallLogging)
        install(ContentNegotiation) { json(json) }

        routing {
            get("/health") {
                call.respond(mapOf("status" to "ok"))
            }
            webSocket("/ws") {
                if (call.request.queryParameters["token"] != token) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "invalid token"))
                    return@webSocket
                }
                val clientId = call.request.queryParameters["uid"] ?: hashCode().toString()
                val outbound = Channel<WireMessage>(Channel.BUFFERED)
                clients.put(clientId, outbound)?.close()
                val sender = launch {
                    for (message in outbound) {
                        runCatching {
                            val frame = Frame.Text(json.encodeToString(WireMessage.serializer(), message))
                            send(frame)
                        }.onFailure { Timber.w(it, "Failed sending to %s", clientId) }
                    }
                }
                try {
                    for (frame in incoming) {
                        val text = (frame as? Frame.Text)?.readText() ?: continue
                        runCatching { json.decodeFromString<WireMessage>(text) }
                            .onSuccess { message ->
                                when (message) {
                                    is WireMessage.AttemptSubmit -> handleAttempt(message)
                                    else -> Timber.d("Ignoring message type %s", message.type)
                                }
                            }
                            .onFailure { Timber.w(it, "Failed to decode frame") }
                    }
                } catch (t: Throwable) {
                    Timber.w(t, "Client %s disconnected", clientId)
                } finally {
                    sender.cancel()
                    outbound.close()
                    clients.remove(clientId, outbound)
                }
            }
            post("/broadcast") {
                val headerToken = call.request.headers[AUTH_HEADER]
                if (headerToken != token) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("ok" to false))
                    return@post
                }
                val payload = call.receiveText()
                val wireMessage = json.decodeFromString<WireMessage>(payload)
                broadcast(wireMessage)
                call.respond(mapOf("ok" to true))
            }
        }
    }

    private fun handleAttempt(message: WireMessage.AttemptSubmit) {
        val outbound = clients[message.uid]
        if (message.selectedJson.length > MAX_SELECTION_BYTES) {
            sendAck(outbound, message.attemptId, accepted = false, reason = "payload_too_large")
            return
        }
        if (isDuplicate(message.attemptId)) {
            sendAck(outbound, message.attemptId, accepted = false, reason = "duplicate")
            return
        }
        if (!_attempts.tryEmit(message)) {
            scope.launch { _attempts.emit(message) }
        }
        sendAck(outbound, message.attemptId, accepted = true)
    }

    private fun sendAck(channel: SendChannel<WireMessage>?, attemptId: String, accepted: Boolean, reason: String? = null) {
        if (channel == null) return
        val ack = WireMessage.Ack(attemptId = attemptId, accepted = accepted, reason = reason)
        val result = channel.trySend(ack)
        if (!result.isSuccess) {
            scope.launch { channel.send(ack) }
        }
    }

    private fun isDuplicate(attemptId: String): Boolean {
        val previous = processedAttempts.putIfAbsent(attemptId, System.currentTimeMillis())
        return previous != null
    }

    private fun pruneAttempts() {
        val cutoff = System.currentTimeMillis() - ATTEMPT_MEMORY_MS
        processedAttempts.entries.removeIf { it.value < cutoff }
    }

    companion object {
        private const val AUTH_HEADER = "X-Session-Token"
        private const val MAX_SELECTION_BYTES = 2_048
        private const val PRUNE_INTERVAL_MS = 60_000L
        private const val ATTEMPT_MEMORY_MS = 5 * 60_000L
    }
}
