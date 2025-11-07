package com.classroom.quizmaster

import com.classroom.quizmaster.data.lan.LanHostManager
import com.classroom.quizmaster.data.lan.WireMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class LanRoundtripTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun attemptDeliveredOnce() = runTest {
        val manager = LanHostManager(json)
        val port = manager.start(token = "token", port = 48999)
        val attemptDeferred = async { manager.attemptSubmissions.first() }
        val client = HttpClient(CIO) { install(WebSockets) }
        client.webSocket(
            request = { url("ws://127.0.0.1:$port/ws?token=token&uid=student") }
        ) {
            val payload = WireMessage.AttemptSubmit(
                attemptId = "attempt-1",
                uid = "student",
                questionId = "q1",
                selectedJson = json.encodeToString(listOf("A")),
                timeMs = 1_000L,
                nonce = "n1"
            )
            send(Frame.Text(json.encodeToString(WireMessage.serializer(), payload)))
        }
        val attempt = withTimeout(2_000) { attemptDeferred.await() }
        assertEquals("attempt-1", attempt.attemptId)
        manager.stop()
        client.close()
    }
}
