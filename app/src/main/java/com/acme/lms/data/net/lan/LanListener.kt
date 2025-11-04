package com.acme.lms.data.net.lan

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LanListener {
    private val sessions = MutableStateFlow<List<String>>(emptyList())

    fun sessions(): Flow<List<String>> = sessions

    // TODO: discover LAN sessions
}
