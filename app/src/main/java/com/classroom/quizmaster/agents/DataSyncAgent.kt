package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

interface DataSyncAgent {
    fun start()
    fun getStatus(): StateFlow<SyncStatus>
    fun triggerSync()
}
