package com.classroom.quizmaster.data.lan

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LanLoopbackTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val hostManager = LanHostManager(json)
    private val client = LanClient(json)

    @AfterEach
    fun tearDown() {
        client.disconnect()
        hostManager.stop()
    }

    @Test
    fun `host receives attempt submissions`() = runBlocking {
        val token = "test-token"
        val port = hostManager.start(token, port = 0)
        val descriptor = LanServiceDescriptor(
            serviceName = "TestService",
            host = "127.0.0.1",
            port = port,
            token = token,
            joinCode = "ABC123",
            timestamp = System.currentTimeMillis()
        )

        val attemptDeferred = async { withTimeout(5_000) { hostManager.attemptSubmissions.first() } }

        val payload = WireMessage.AttemptSubmit(
            attemptId = "attempt-1",
            uid = "user-1",
            questionId = "q1",
            selectedJson = json.encodeToString(listOf("A")),
            nickname = "Tester",
            timeMs = 1_500,
            nonce = "nonce"
        )

        client.sendAttempt(descriptor, payload)
        val received = attemptDeferred.await()
        assertEquals(payload.attemptId, received.attemptId)
        assertEquals(payload.uid, received.uid)
    }
}
