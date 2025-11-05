package com.acme.lms.agents.impl

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.acme.lms.agents.DataSyncAgent
import com.example.lms.core.model.SyncStatus
import com.acme.lms.workers.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSyncAgentImpl @Inject constructor(
    private val context: Context
) : DataSyncAgent {

    private val workManager = WorkManager.getInstance(context)

    override fun start() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun getStatus(): Flow<SyncStatus> {
        return workManager.getWorkInfosForUniqueWorkFlow("sync").map {
            val workInfo = it.firstOrNull()
            SyncStatus(
                inProgress = workInfo?.state?.isFinished == false,
                lastSuccessAt = null, // TODO: Get actual last success time from SyncOrchestrator
                pendingItems = 0 // TODO: Get actual pending items count from SyncOrchestrator
            )
        }
    }

    override fun triggerSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }
}
