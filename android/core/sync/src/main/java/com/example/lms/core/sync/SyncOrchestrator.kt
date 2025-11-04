package com.example.lms.core.sync

import com.example.lms.core.common.runCatchingResult
import com.example.lms.core.database.dao.OutboxDao
import com.example.lms.core.database.entity.OutboxEntity
import com.example.lms.core.model.Class
import com.example.lms.core.model.LmsResult
import com.example.lms.core.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ClassRemoteDataSource {
    suspend fun fetchClasses(org: String): LmsResult<List<Class>>
}

class SyncOrchestrator(
    private val outboxDao: OutboxDao,
    private val remoteDataSource: ClassRemoteDataSource,
    private val scope: CoroutineScope,
) {
    private val status = MutableStateFlow(SyncStatus(inProgress = false, lastSuccessAt = null, pendingItems = 0))

    fun status(): Flow<SyncStatus> = status

    fun start() {
        scope.launch {
            outboxDao.observeOutbox().collect { queue ->
                status.value = status.value.copy(pendingItems = queue.size)
            }
        }
    }

    fun triggerSync(org: String) {
        scope.launch {
            status.value = status.value.copy(inProgress = true)
            val result = syncOutbox()
            if (result is LmsResult.Success) {
                remoteDataSource.fetchClasses(org)
            }
            status.value = status.value.copy(inProgress = false, lastSuccessAt = System.currentTimeMillis())
        }
    }

    private suspend fun syncOutbox(): LmsResult<Unit> = runCatchingResult {
        val pending = outboxDao.observeOutbox().first()
        if (pending.isNotEmpty()) {
            outboxDao.dequeue(pending.map { it.id })
        }
    }

    suspend fun enqueue(type: String, payload: String) {
        withContext(Dispatchers.IO) {
            outboxDao.enqueue(
                OutboxEntity(
                    payloadType = type,
                    payload = payload,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}

