package com.classroom.quizmaster.data.lan

sealed interface LanClientStatus {
    data object Disconnected : LanClientStatus
    data class Connecting(val endpoint: LanServiceDescriptor) : LanClientStatus
    data class Connected(val endpoint: LanServiceDescriptor) : LanClientStatus
    data class Reconnecting(val endpoint: LanServiceDescriptor) : LanClientStatus
}
