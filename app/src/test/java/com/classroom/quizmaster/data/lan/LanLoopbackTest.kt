package com.classroom.quizmaster.data.lan

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import org.robolectric.junit5.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(manifest = Config.NONE)
class LanLoopbackTest {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var host: LanHostServer
    private lateinit var client: LanClient

    @BeforeEach
    fun setUp() {
        host = LanHostServer(json)
        client = LanClient(json)
    }

    @AfterEach
    fun tearDown() {
        client.disconnect()
        host.stop()
    }

    @Test
    fun `attempt round trip stays under latency budget`() = runBlocking {
        val token = "token"
        val port = host.start(token, 0)
        val descriptor = LanServiceDescriptor(
            serviceName = "TestHost",
            host = "127.0.0.1",
            port = port,
            token = token,
            joinCode = "ABC123",
            timestamp = System.currentTimeMillis()
        )

        val connected = async { client.status.filterIsInstance<LanClientStatus.Connected>().first() }
        client.connect(descriptor, uid = "student-1")
        connected.await()

        val ackDeferred = async {
            client.messages
                .filterIsInstance<WireMessage.Ack>()
                .first { it.attemptId == "attempt-1" }
        }
        val inboundDeferred = async {
            host.attemptSubmissions.first { it.attemptId == "attempt-1" }
        }

        val payload = WireMessage.AttemptSubmit(
            attemptId = "attempt-1",
            uid = "student-1",
            questionId = "q1",
            selectedJson = json.encodeToString(listOf("A")),
            nickname = "Nova",
            timeMs = 1_500,
            nonce = "nonce"
        )

        val startNanos = System.nanoTime()
        assertTrue(client.sendAttempt(payload))
        val ack = withTimeout(5_000) { ackDeferred.await() }
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0
        assertTrue(elapsedMs <= 150.0, "Ack exceeded 150 ms: $elapsedMs")
        assertTrue(ack.accepted)

        val inbound = withTimeout(5_000) { inboundDeferred.await() }
        assertEquals(payload.attemptId, inbound.attemptId)
        assertEquals(payload.uid, inbound.uid)
    }
}
