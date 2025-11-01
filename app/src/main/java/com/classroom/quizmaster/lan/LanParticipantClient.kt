package com.classroom.quizmaster.lan

import com.classroom.quizmaster.agents.AnswerPayload
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LanParticipantClient(
    private val config: LanConfiguration = LanConfiguration()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val _snapshots = MutableStateFlow<LanServerMessage.Snapshot?>(null)
    val snapshots: StateFlow<LanServerMessage.Snapshot?> = _snapshots.asStateFlow()

    private val _answerAcks = MutableSharedFlow<LanServerMessage.AnswerAck>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val answerAcks: SharedFlow<LanServerMessage.AnswerAck> = _answerAcks.asSharedFlow()

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var readerJob: Job? = null
    private val writeMutex = Mutex()

    suspend fun join(
        host: String,
        sessionId: String,
        nickname: String,
        timeoutMs: Int = 5000
    ): LanServerMessage.JoinAck {
        return try {
            disconnect()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, config.serverPort), timeoutMs)
            this.socket = socket
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

            val joinMessage = LanClientMessage.JoinRequest(sessionId = sessionId, nickname = nickname)
            sendRaw(json.encodeToString(LanClientMessage.serializer(), joinMessage))

            val ackLine = reader?.readLine()
                ?: return LanServerMessage.JoinAck(false, reason = "No response")
            val ack = json.decodeFromString(LanServerMessage.serializer(), ackLine)
            val joinAck = ack as? LanServerMessage.JoinAck
                ?: return LanServerMessage.JoinAck(false, reason = "Unexpected response")

            if (joinAck.accepted) {
                startReader()
            } else {
                disconnect()
            }
            joinAck
        } catch (ex: Exception) {
            disconnect()
            LanServerMessage.JoinAck(false, reason = ex.message ?: "Connection error")
        }
    }

    fun sendAnswer(sessionId: String, studentId: String, payload: AnswerPayload) {
        val message = LanClientMessage.Answer(sessionId, studentId, payload)
        sendRaw(json.encodeToString(LanClientMessage.serializer(), message))
    }

    fun requestSnapshot(sessionId: String) {
        val ping = LanClientMessage.Ping(sessionId)
        sendRaw(json.encodeToString(LanClientMessage.serializer(), ping))
    }

    fun disconnect() {
        readerJob?.cancel()
        readerJob = null
        kotlin.runCatching { socket?.close() }
        socket = null
        reader = null
        writer = null
    }

    fun shutdown() {
        disconnect()
        job.cancel()
    }

    private fun startReader() {
        val reader = reader ?: return
        readerJob = scope.launch {
            try {
                while (true) {
                    val line = reader.readLine() ?: break
                    val message = runCatching {
                        json.decodeFromString(LanServerMessage.serializer(), line)
                    }.getOrNull() ?: continue
                    when (message) {
                        is LanServerMessage.Snapshot -> _snapshots.value = message
                        is LanServerMessage.AnswerAck -> _answerAcks.emit(message)
                        is LanServerMessage.JoinAck -> {
                            // join ack handled earlier
                        }
                        is LanServerMessage.Announcement -> {
                            // ignore on client connection
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                disconnect()
            }
        }
    }

    private fun sendRaw(message: String) {
        scope.launch {
            writeMutex.withLock {
                val writer = writer ?: return@withLock
                kotlin.runCatching {
                    writer.write(message)
                    writer.newLine()
                    writer.flush()
                }
            }
        }
    }
}
