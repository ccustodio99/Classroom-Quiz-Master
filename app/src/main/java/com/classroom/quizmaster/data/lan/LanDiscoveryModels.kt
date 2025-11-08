package com.classroom.quizmaster.data.lan

data class LanServiceDescriptor(
    val serviceName: String,
    val host: String,
    val port: Int,
    val token: String,
    val joinCode: String,
    val timestamp: Long
) {
    val wsUri: String get() = "ws://$host:$port/ws"
    val qrUri: String get() = "$wsUri?token=$token"
}

sealed interface LanDiscoveryEvent {
    data class ServiceFound(val descriptor: LanServiceDescriptor) : LanDiscoveryEvent
    data class Error(val message: String) : LanDiscoveryEvent
    data object Timeout : LanDiscoveryEvent
}
