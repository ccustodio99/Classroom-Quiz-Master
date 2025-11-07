package com.classroom.quizmaster.data.lan

import com.classroom.quizmaster.BuildConfig
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
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
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class LanHostManager @Inject constructor(
    private val json: Json
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: ApplicationEngine? = null
    private val clients = ConcurrentHashMap<String, Channel<WireMessage>>()
    private val _attempts = MutableSharedFlow<WireMessage.AttemptSubmit>(extraBufferCapacity = 32)
    val attemptSubmissions: SharedFlow<WireMessage.AttemptSubmit> = _attempts.asSharedFlow()
    private val processedAttempts = ConcurrentHashMap<String, Long>()
    private val cleanupJob: Job = scope.launch {
        while (isActive) {
            delay(60_000)
            pruneAttempts()
        }
    }

    fun start(token: String, port: Int = BuildConfig.LAN_DEFAULT_PORT): Int {
        if (server != null) {
            return server?.environment?.connectors?.firstOrNull()?.port ?: port
        }
        val engine = embeddedServer(CIO, port = port, host = BuildConfig.LAN_DEFAULT_HOST) {
            configureServer(token)
        }
        engine.start()
        server = engine
        val boundPort = engine.environment.connectors.firstOrNull()?.port ?: port
        Timber.i("LAN host started on port $boundPort")
        return boundPort
    }

    fun stop() {
        server?.stop()
        server = null
        clients.values.forEach { it.close() }
        clients.clear()
        processedAttempts.clear()
        Timber.i("LAN host stopped")
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
                runCatching { channel.send(WireMessage.SystemNotice("Removed by host")) }
                channel.close()
            }
        }
    }

    private fun Application.configureServer(token: String) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
            pingPeriodMillis = 15_000
            timeoutMillis = 30_000
            maxFrameSize = 64 * 1024L
        }
        install(CallLogging)
        install(ContentNegotiation) {
            json(json)
        }
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
                    for (msg in outbound) {
                        sendSerialized(msg)
                    }
                }
                try {
                    while (true) {
                        val message = receiveDeserialized<WireMessage>()
                        if (message is WireMessage.AttemptSubmit) {
                            if (message.selectedJson.length > MAX_SELECTION_BYTES) {
                                sendSerialized(
                                    WireMessage.Ack(
                                        message.attemptId,
                                        accepted = false,
                                        reason = "payload_too_large"
                                    )
                                )
                                continue
                            }
                            if (isDuplicate(message.attemptId)) {
                                sendSerialized(WireMessage.Ack(message.attemptId, accepted = false, reason = "duplicate"))
                            } else {
                                _attempts.emit(message)
                                sendSerialized(WireMessage.Ack(message.attemptId, accepted = true))
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Timber.w(ex, "Client $clientId disconnected")
                } finally {
                    clients.remove(clientId)?.close()
                }
            }
            post("/broadcast") {
                val payload = call.receive<String>()
                val wire = json.decodeFromString(WireMessage.serializer(), payload)
                broadcast(wire)
                call.respond(mapOf("ok" to true))
            }
        }
    }

    private fun isDuplicate(attemptId: String): Boolean {
        val previous = processedAttempts.putIfAbsent(attemptId, System.currentTimeMillis())
        return previous != null
    }

    private fun pruneAttempts() {
        val cutoff = System.currentTimeMillis() - 5 * 60_000
        processedAttempts.entries.removeIf { it.value < cutoff }
    }

    companion object {
        private const val MAX_SELECTION_BYTES = 2_048
    }
}
