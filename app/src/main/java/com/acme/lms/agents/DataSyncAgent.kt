package com.acme.lms.agents

import com.acme.lms.data.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

interface DataSyncAgent {
    fun start()
    fun getStatus(): StateFlow<SyncStatus>
    fun triggerSync()
}
