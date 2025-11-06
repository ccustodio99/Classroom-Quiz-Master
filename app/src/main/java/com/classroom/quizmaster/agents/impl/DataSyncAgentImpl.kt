package com.classroom.quizmaster.agents.impl

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.classroom.quizmaster.agents.DataSyncAgent
import com.classroom.quizmaster.data.model.SyncStatus
import com.classroom.quizmaster.workers.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PERIODIC_WORK_NAME = "data-sync"
private const val TRIGGERED_WORK_NAME = "data-sync-now"
internal const val SYNC_OUTPUT_COMPLETED_AT = "completedAt"

@Singleton
class DataSyncAgentImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DataSyncAgent {

    private val workManager = WorkManager.getInstance(context)
    private val statusQuery = WorkQuery.Builder.fromUniqueWorkNames(
        listOf(PERIODIC_WORK_NAME, TRIGGERED_WORK_NAME)
    ).build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _status = MutableStateFlow(
        SyncStatus(inProgress = false, lastSuccessAt = null, pendingItems = 0)
    )

    init {
        workManager.getWorkInfosFlow(statusQuery)
            .map { infos ->
                val running = infos.any { info ->
                    info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED
                }
                val lastSuccess = infos.mapNotNull { info ->
                    info.outputData.getLong(SYNC_OUTPUT_COMPLETED_AT, 0L).takeIf { it > 0 }
                }.maxOrNull()
                val pending = infos.count { info ->
                    info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.BLOCKED
                }
                SyncStatus(
                    inProgress = running,
                    lastSuccessAt = lastSuccess,
                    pendingItems = pending
                )
            }
            .onEach { _status.value = it }
            .launchIn(scope)
    }

    override fun start() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun getStatus(): StateFlow<SyncStatus> = _status

    override fun triggerSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        workManager.enqueueUniqueWork(
            TRIGGERED_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
