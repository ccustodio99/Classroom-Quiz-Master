package com.classroom.quizmaster.data.lan

import com.classroom.quizmaster.BuildConfig
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.close
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
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

@Singleton
class LanHostServer @Inject constructor(
    private val json: Json
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: ApplicationEngine? = null
    private val clients = ConcurrentHashMap<String, Channel<WireMessage>>()
    private val processedAttempts = ConcurrentHashMap<String, Long>()
    private val _attempts = MutableSharedFlow<WireMessage.AttemptSubmit>(extraBufferCapacity = 32)
    val attemptSubmissions: SharedFlow<WireMessage.AttemptSubmit> = _attempts.asSharedFlow()
    private val cleanupJob: Job = scope.launch {
        while (isActive) {
            delay(PRUNE_INTERVAL_MS)
            pruneAttempts()
        }
    }

    fun start(token: String, requestedPort: Int = BuildConfig.LAN_DEFAULT_PORT): Int {
        server?.let { existing ->
            return existing.environment.connectors.firstOrNull()?.port ?: requestedPort
        }
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
        Timber.i("LAN host server started on $boundPort")
        return boundPort
    }

    fun stop() {
        server?.stop()
        server = null
        clients.values.forEach(Channel<WireMessage>::close)
        clients.clear()
        processedAttempts.clear()
        Timber.i("LAN host server stopped")
    }

    suspend fun broadcast(message: WireMessage) {
        clients.values.forEach { channel ->
            runCatching { channel.send(message) }
                .onFailure { channel.close() }
        }
    }

    fun kick(uid: String) {
        clients.remove(uid)?.let { channel ->
            scope.launch {
                runCatching { channel.send(WireMessage.SystemNotice("removed_by_host")) }
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
                val queryToken = call.request.queryParameters["token"]
                if (queryToken != token) {
                    close()
                    return@webSocket
                }
                val clientId = call.request.queryParameters["uid"] ?: hashCode().toString()
                val outbound = Channel<WireMessage>(Channel.BUFFERED)
                clients[clientId] = outbound
                scope.launch {
                    for (message in outbound) {
                        runCatching { sendSerialized(message) }
                            .onFailure { Timber.w(it, "Failed sending to $clientId") }
                    }
                }
                try {
                    while (true) {
                        val message = receiveDeserialized<WireMessage>()
                        when (message) {
                            is WireMessage.AttemptSubmit -> handleAttempt(message)
                            else -> Timber.d("Ignoring message type ${message.type}")
                        }
                    }
                } catch (t: Throwable) {
                    Timber.w(t, "Client $clientId disconnected")
                } finally {
                    clients.remove(clientId)?.close()
                }
            }
            post("/broadcast") {
                val headerToken = call.request.headers[AUTH_HEADER]
                if (headerToken != token) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("ok" to false))
                    return@post
                }
                val wireMessage = json.decodeFromString<WireMessage>(call.receive())
                broadcast(wireMessage)
                call.respond(mapOf("ok" to true))
            }
        }
    }

    private suspend fun handleAttempt(message: WireMessage.AttemptSubmit) {
        if (message.selectedJson.length > MAX_SELECTION_BYTES) {
            clients[message.uid]?.send(
                WireMessage.Ack(message.attemptId, accepted = false, reason = "payload_too_large")
            )
            return
        }
        if (isDuplicate(message.attemptId)) {
            clients[message.uid]?.send(
                WireMessage.Ack(message.attemptId, accepted = false, reason = "duplicate")
            )
            return
        }
        _attempts.emit(message)
        clients[message.uid]?.send(WireMessage.Ack(message.attemptId, accepted = true))
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
