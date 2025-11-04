package com.classroom.quizmaster.domain.agent.impl

import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.data.local.PendingSync
import com.classroom.quizmaster.domain.agent.DataSyncAgent
import com.classroom.quizmaster.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DataSyncAgentImpl(
    private val localData: BlueprintLocalDataSource,
    private val syncDispatcher: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : DataSyncAgent {

    private val status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    private var started = false

    override fun start() {
        if (started) return
        started = true
        syncDispatcher.launch {
            localData.snapshot.collect { snapshot ->
                if (snapshot.pendingSync.isNotEmpty() && status.value == SyncStatus.Idle) {
                    triggerSync()
                }
            }
        }
    }

    override fun getStatus(): StateFlow<SyncStatus> = status

    override fun triggerSync() {
        syncDispatcher.launch {
            val operations = localData.snapshot.value.pendingSync
            if (operations.isEmpty()) {
                status.value = SyncStatus.Success(System.currentTimeMillis())
                return@launch
            }
            status.value = SyncStatus.InProgress
            processOperations(operations)
        }
    }

    private suspend fun processOperations(operations: List<PendingSync>) {
        try {
            for (operation in operations) {
                // Simulate network call with delay. In a real implementation, this is where
                // Firebase or REST API calls would be executed.
                delay(150)
                localData.markSyncSuccess(operation.id)
            }
            status.value = SyncStatus.Success(System.currentTimeMillis())
        } catch (error: Exception) {
            operations.firstOrNull()?.let { localData.incrementSyncAttempts(it.id) }
            status.value = SyncStatus.Error(error.message.orEmpty())
        }
    }
}
