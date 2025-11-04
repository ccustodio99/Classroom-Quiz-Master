package com.acme.lms.data.net.lan

class LanBroadcaster {
    private var running = false

    fun start(sessionId: String) {
        running = true
        // TODO: advertise session over mDNS/NSD
    }

    fun stop() {
        running = false
    }

    fun isRunning(): Boolean = running
}
