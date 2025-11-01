package com.classroom.quizmaster.lan

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LanDiscoveryClient(
    private val config: LanConfiguration = LanConfiguration()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    suspend fun discover(timeoutMillis: Long = 3_000): List<LanServerMessage.Announcement> = withContext(Dispatchers.IO) {
        val socket = DatagramSocket().apply {
            soTimeout = 500
            broadcast = true
        }
        val request = DiscoveryRequest()
        val encoded = json.encodeToString(DiscoveryRequest.serializer(), request).toByteArray()
        val packet = DatagramPacket(
            encoded,
            encoded.size,
            InetAddress.getByName("255.255.255.255"),
            config.discoveryPort
        )
        kotlin.runCatching { socket.send(packet) }

        val found = mutableListOf<LanServerMessage.Announcement>()
        val buffer = ByteArray(1024)
        val endTime = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < endTime) {
            val response = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(response)
                val message = json.decodeFromString(
                    LanServerMessage.serializer(),
                    String(response.data, 0, response.length)
                )
                if (message is LanServerMessage.Announcement) {
                    if (found.none { it.sessionId == message.sessionId }) {
                        found += message.copy(host = response.address.hostAddress)
                    }
                }
            } catch (ex: SocketTimeoutException) {
                // continue waiting
            }
        }
        socket.close()
        found
    }
}
