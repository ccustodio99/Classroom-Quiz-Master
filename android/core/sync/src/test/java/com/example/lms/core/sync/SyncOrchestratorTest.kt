package com.example.lms.core.sync

import com.example.lms.core.database.dao.OutboxDao
import com.example.lms.core.database.entity.OutboxEntity
import com.example.lms.core.model.LmsResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SyncOrchestratorTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val outboxFlow = MutableStateFlow<List<OutboxEntity>>(emptyList())
    private val fakeOutboxDao = object : OutboxDao {
        override suspend fun enqueue(entity: OutboxEntity): Long {
            outboxFlow.value = outboxFlow.value + entity.copy(id = (outboxFlow.value.size + 1).toLong())
            return entity.id
        }

        override fun observeOutbox(): Flow<List<OutboxEntity>> = outboxFlow.asStateFlow()

        override suspend fun deleteByIds(ids: List<Long>) {
            outboxFlow.value = outboxFlow.value.filterNot { it.id in ids }
        }

        override suspend fun dequeue(ids: List<Long>) {
            deleteByIds(ids)
        }
    }
    private val fakeRemote = object : ClassRemoteDataSource {
        override suspend fun fetchClasses(org: String) = LmsResult.Success(emptyList())
    }

    @Test
    fun `enqueue updates pending count`() = runTest(dispatcher) {
        val orchestrator = SyncOrchestrator(outboxDao = fakeOutboxDao, remoteDataSource = fakeRemote, scope = scope)
        orchestrator.start()
        orchestrator.enqueue("test", "{\"value\":1}")
        advanceUntilIdle()
        val status = orchestrator.status().first()
        assertEquals(1, status.pendingItems)
    }
}

