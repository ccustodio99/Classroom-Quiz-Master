package com.classroom.quizmaster.lan

import com.classroom.quizmaster.agents.AnswerPayload
import com.classroom.quizmaster.agents.LiveSnapshot
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LiveSessionLanHost(
    private val sessionId: String,
    private val moduleId: String,
    private val config: LanConfiguration,
    private val onJoin: suspend (String) -> LanJoinAck,
    private val onAnswer: suspend (AnswerPayload) -> LanAnswerAck,
    private val snapshotProvider: () -> LiveSnapshot?
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connections = ConcurrentHashMap<Int, LanConnection>()
    private val connectionIdGenerator = Mutex()
    private var nextId = 1

    fun start() {
        scope.launch { acceptLoop() }
        scope.launch { discoveryLoop() }
    }

    fun stop() {
        scope.cancel()
        connections.values.forEach { it.close() }
    }

    fun broadcast(snapshot: LiveSnapshot) {
        val message = LanServerMessage.Snapshot(snapshot)
        val encoded = json.encodeToString(LanServerMessage.serializer(), message)
        connections.values.forEach { connection ->
            connection.send(encoded)
        }
    }

    fun notifyJoin(student: com.classroom.quizmaster.domain.model.Student) {
        // Broadcast updated snapshot so participants see the roster
        snapshotProvider()?.let { broadcast(it) }
    }

    private suspend fun acceptLoop() {
        withContext(Dispatchers.IO) {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(config.serverPort)
                while (scope.isActive) {
                    val socket = serverSocket.accept()
                    val connectionId = connectionIdGenerator.withLock { nextId++ }
                    val connection = LanConnection(connectionId, socket)
                    connections[connectionId] = connection
                    scope.launch { handleConnection(connection) }
                }
            } catch (_: CancellationException) {
                // ignored
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                try {
                    serverSocket?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun handleConnection(connection: LanConnection) {
        try {
            while (scope.isActive && connection.isOpen()) {
                val line = connection.reader.readLine() ?: break
                val message = runCatching {
                    json.decodeFromString(LanClientMessage.serializer(), line)
                }.getOrNull() ?: continue
                when (message) {
                    is LanClientMessage.JoinRequest -> handleJoin(connection, message)
                    is LanClientMessage.Answer -> handleAnswer(connection, message)
                    is LanClientMessage.Ping -> sendSnapshot(connection)
                }
            }
        } catch (_: CancellationException) {
        } catch (_: Exception) {
        } finally {
            connection.close()
            connections.remove(connection.id)
        }
    }

    private fun sendSnapshot(connection: LanConnection) {
        val snapshot = snapshotProvider() ?: return
        val message = LanServerMessage.Snapshot(snapshot)
        val encoded = json.encodeToString(LanServerMessage.serializer(), message)
        connection.send(encoded)
    }

    private fun handleJoin(connection: LanConnection, request: LanClientMessage.JoinRequest) {
        if (request.sessionId != sessionId) {
            val encoded = json.encodeToString(
                LanServerMessage.serializer(),
                LanServerMessage.JoinAck(false, reason = "Invalid session")
            )
            connection.send(encoded)
            return
        }
        scope.launch {
            val result = onJoin(request.nickname)
            val encoded = json.encodeToString(
                LanServerMessage.serializer(),
                LanServerMessage.JoinAck(
                    accepted = result.accepted,
                    studentId = result.studentId,
                    displayName = result.displayName,
                    reason = result.reason
                )
            )
            connection.send(encoded)
            if (result.accepted) {
                sendSnapshot(connection)
            }
        }
    }

    private fun handleAnswer(connection: LanConnection, message: LanClientMessage.Answer) {
        if (message.sessionId != sessionId) {
            val encoded = json.encodeToString(
                LanServerMessage.serializer(),
                LanServerMessage.AnswerAck(false, reason = "Invalid session")
            )
            connection.send(encoded)
            return
        }
        scope.launch {
            val ack = onAnswer(message.answer.copy(studentId = message.studentId))
            val encoded = json.encodeToString(
                LanServerMessage.serializer(),
                LanServerMessage.AnswerAck(ack.accepted, ack.reason)
            )
            connection.send(encoded)
            if (ack.accepted) {
                sendSnapshot(connection)
            }
        }
    }

    private suspend fun discoveryLoop() {
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(config.discoveryPort)
                socket.broadcast = true
                while (scope.isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val incoming = kotlin.runCatching {
                        json.decodeFromString(DiscoveryRequest.serializer(),
                            String(packet.data, 0, packet.length)
                        )
                    }.getOrNull() ?: continue
                    if (incoming.requestId.isBlank()) continue
                    val snapshot = snapshotProvider()
                    val announcement = LanServerMessage.Announcement(
                        sessionId = sessionId,
                        moduleId = moduleId,
                        host = localAddress(),
                        port = config.serverPort,
                        participants = snapshot?.participants?.size ?: 0
                    )
                    val encoded = json.encodeToString(LanServerMessage.serializer(), announcement)
                    val bytes = encoded.toByteArray()
                    val response = DatagramPacket(
                        bytes,
                        bytes.size,
                        packet.address,
                        packet.port
                    )
                    socket.send(response)
                }
            } catch (_: SocketException) {
            } catch (_: CancellationException) {
            } catch (_: Exception) {
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun localAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            interfaces.toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { address ->
                    !address.isLoopbackAddress && address is java.net.Inet4Address
                }
                ?.hostAddress ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    private inner class LanConnection(
        val id: Int,
        private val socket: Socket
    ) {
        private val writerMutex = Mutex()
        val reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
        private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

        fun send(message: String) {
            scope.launch {
                writerMutex.withLock {
                    kotlin.runCatching {
                        writer.write(message)
                        writer.newLine()
                        writer.flush()
                    }
                }
            }
        }

        fun isOpen(): Boolean = !socket.isClosed

        fun close() {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }
}
