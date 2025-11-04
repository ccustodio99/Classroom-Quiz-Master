package com.classroom.quizmaster.domain.agent.impl

import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.data.local.ClassworkBundle
import com.classroom.quizmaster.data.local.PendingSync
import com.classroom.quizmaster.data.remote.FirestoreSyncService
import com.classroom.quizmaster.domain.agent.DataSyncAgent
import com.classroom.quizmaster.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DataSyncAgentImpl(
    private val localData: BlueprintLocalDataSource,
    private val firestoreSyncService: FirestoreSyncService,
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
            status.value = SyncStatus.InProgress
            val operations = localData.snapshot.value.pendingSync
            try {
                if (operations.isNotEmpty()) {
                    pushPendingOperations(operations)
                }
                pullRemoteSnapshot()
                status.value = SyncStatus.Success(System.currentTimeMillis())
            } catch (error: Exception) {
                status.value = SyncStatus.Error(error.message.orEmpty())
            }
        }
    }

    private suspend fun pushPendingOperations(operations: List<PendingSync>) {
        for (operation in operations) {
            try {
                firestoreSyncService.pushOperation(operation)
                localData.markSyncSuccess(operation.id)
            } catch (error: Exception) {
                localData.incrementSyncAttempts(operation.id)
                throw error
            }
        }
    }

    private suspend fun pullRemoteSnapshot() {
        val snapshot = firestoreSyncService.fetchSnapshot()
        snapshot.classes.forEach { localData.upsertClass(it) }
        snapshot.rosters.forEach { (classId, entries) ->
            entries.forEach { localData.upsertRosterEntry(classId, it) }
        }
        snapshot.classwork.forEach { (classId, items) ->
            items.forEach { classwork ->
                localData.upsertClasswork(ClassworkBundle(item = classwork))
            }
        }
        snapshot.attempts.forEach { localData.recordAttempt(it) }
        snapshot.submissions.forEach { localData.recordSubmission(it) }
        snapshot.liveSessions.forEach { localData.upsertLiveSession(it) }
        snapshot.liveResponses.forEach { (_, responses) ->
            responses.forEach { localData.recordLiveResponse(it) }
        }
    }
}
