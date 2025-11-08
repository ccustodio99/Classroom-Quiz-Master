package com.classroom.quizmaster.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.classroom.quizmaster.data.local.dao.OpLogDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import timber.log.Timber

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext context: Context,
    private val opLogDao: OpLogDao,
    private val applicationScope: CoroutineScope
) {
    private val workManager = WorkManager.getInstance(context)
    private val queueObserverStarted = AtomicBoolean(false)

    private val networkConstraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedule() {
        schedulePeriodic()
        observeQueue()
        kickstartIfBacklogExists()
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, PERIODIC_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    FirestoreSyncWorker.KEY_REASON to FirestoreSyncWorker.REASON_PERIODIC
                )
            )
            .addTag(TAG_PERIODIC)
            .build()
        workManager.enqueueUniquePeriodicWork(
            FirestoreSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueNow(reason: String = FirestoreSyncWorker.REASON_MANUAL, pendingCountHint: Int? = null) {
        val dataBuilder = androidx.work.Data.Builder()
            .putString(FirestoreSyncWorker.KEY_REASON, reason)
        pendingCountHint?.let { dataBuilder.putInt(FirestoreSyncWorker.KEY_PENDING_COUNT, it) }
        val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ON_DEMAND_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(dataBuilder.build())
            .addTag(TAG_IMMEDIATE)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_ON_DEMAND_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    private fun observeQueue() {
        if (!queueObserverStarted.compareAndSet(false, true)) {
            return
        }
        applicationScope.launch(Dispatchers.IO) {
            var previous = -1
            opLogDao.observePendingCount().collect { count ->
                val previousSnapshot = previous
                previous = count
                if (count > 0 && (previousSnapshot <= 0 || count > previousSnapshot)) {
                    Timber.d("OpLog pending count=%d, enqueueing sync", count)
                    enqueueNow(FirestoreSyncWorker.REASON_QUEUE_FLUSH, count)
                }
            }
        }
    }

    private fun kickstartIfBacklogExists() {
        applicationScope.launch(Dispatchers.IO) {
            runCatching { opLogDao.pending(limit = 1) }
                .onSuccess { pending ->
                    if (pending.isNotEmpty()) {
                        Timber.d("Backlog detected at startup (%d items)", pending.size)
                        enqueueNow(FirestoreSyncWorker.REASON_STARTUP, pending.size)
                    }
                }
                .onFailure { Timber.w(it, "Failed to inspect OpLog backlog") }
        }
    }

    private companion object {
        private const val SYNC_INTERVAL_MINUTES = 15L
        private const val PERIODIC_BACKOFF_MINUTES = 15L
        private const val ON_DEMAND_BACKOFF_SECONDS = 30L
        private const val TAG_PERIODIC = "periodic_sync"
        private const val TAG_IMMEDIATE = "sync_now"
        private const val UNIQUE_ON_DEMAND_WORK_NAME = "${FirestoreSyncWorker.UNIQUE_NAME}_on_demand"
    }
}
