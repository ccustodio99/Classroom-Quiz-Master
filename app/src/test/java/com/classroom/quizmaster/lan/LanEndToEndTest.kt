package com.classroom.quizmaster.lan

import com.classroom.quizmaster.data.lan.LanClient
import com.classroom.quizmaster.data.lan.LanClientStatus
import com.classroom.quizmaster.data.lan.LanHostServer
import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.data.lan.WireMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanEndToEndTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `host receives attempt from client`() = runBlocking {
        val host = LanHostServer(json)
        val token = "test-token"
        val port = host.start(token, requestedPort = 0)
        val descriptor = LanServiceDescriptor(
            serviceName = "test-service",
            host = "127.0.0.1",
            port = port,
            token = token,
            joinCode = "JOIN01",
            timestamp = System.currentTimeMillis()
        )
        val client = LanClient(json)

        val connected = CompletableDeferred<Unit>()
        val statusJob = launch {
            client.status.collect { status ->
                if (status is LanClientStatus.Connected && !connected.isCompleted) {
                    connected.complete(Unit)
                }
            }
        }
        client.connect(descriptor, uid = "tester")
        withTimeout(5_000) { connected.await() }

        val receivedAttempt = CompletableDeferred<WireMessage.AttemptSubmit>()
        val hostJob: Job = launch {
            host.attemptSubmissions.collect { attempt ->
                if (!receivedAttempt.isCompleted) {
                    receivedAttempt.complete(attempt)
                }
            }
        }

        val payload = WireMessage.AttemptSubmit(
            attemptId = "attempt-1",
            uid = "tester",
            questionId = "q1",
            selectedJson = json.encodeToString(listOf("A")),
            nickname = "Tester",
            timeMs = 500,
            nonce = "attempt-1"
        )

        assertTrue("client should send attempt", client.sendAttempt(payload))
        val delivered = withTimeout(5_000) { receivedAttempt.await() }
        assertEquals(payload.attemptId, delivered.attemptId)
        assertEquals(payload.uid, delivered.uid)
        assertEquals(payload.questionId, delivered.questionId)

        client.disconnect()
        statusJob.cancelAndJoin()
        hostJob.cancelAndJoin()
        host.stop()
    }
}
