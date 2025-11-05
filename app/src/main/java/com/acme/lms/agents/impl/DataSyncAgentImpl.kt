package com.acme.lms.agents.impl

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.acme.lms.agents.DataSyncAgent
import com.acme.lms.data.model.SyncStatus
import com.acme.lms.workers.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PERIODIC_WORK_NAME = "data-sync"
private const val TRIGGERED_WORK_NAME = "data-sync-now"
internal const val SYNC_OUTPUT_COMPLETED_AT = "completedAt"

@Singleton
class DataSyncAgentImpl @Inject constructor(
    private val context: Context
) : DataSyncAgent {

    private val workManager = WorkManager.getInstance(context)
    private val statusQuery = WorkQuery.Builder.fromUniqueWorkNames(
        listOf(PERIODIC_WORK_NAME, TRIGGERED_WORK_NAME)
    ).build()

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

    override fun getStatus(): Flow<SyncStatus> {
        return workManager.getWorkInfosFlow(statusQuery).map { infos ->
            val running = infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            val lastSuccess = infos.mapNotNull { info ->
                info.outputData.getLong(SYNC_OUTPUT_COMPLETED_AT, 0L).takeIf { it > 0 }
            }.maxOrNull()
            SyncStatus(
                inProgress = running,
                lastSuccessAt = lastSuccess,
                pendingItems = 0 // TODO: surface pending items from SyncRepo when available
            )
        }
    }

    override fun triggerSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        workManager.enqueueUniqueWork(
            TRIGGERED_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
