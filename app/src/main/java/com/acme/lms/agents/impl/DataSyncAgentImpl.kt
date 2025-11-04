package com.acme.lms.agents.impl

import com.acme.lms.agents.DataSyncAgent
import com.acme.lms.data.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSyncAgentImpl @Inject constructor() : DataSyncAgent {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val status = MutableStateFlow(SyncStatus.IDLE)

    override fun start() {
        if (status.value == SyncStatus.RUNNING) return
        triggerSync()
    }

    override fun getStatus(): StateFlow<SyncStatus> = status

    override fun triggerSync() {
        scope.launch {
            status.value = SyncStatus.RUNNING
            try {
                delay(500) // simulate sync work
                status.value = SyncStatus.IDLE
            } catch (t: Throwable) {
                status.value = SyncStatus.ERROR
            }
        }
    }
}
