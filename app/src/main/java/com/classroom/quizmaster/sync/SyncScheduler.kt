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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodic() {
        val constraints = defaultConstraints()
        val request = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_PERIODIC)
            .build()
        workManager.enqueueUniquePeriodicWork(
            FirestoreSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun enqueueNow(reason: String = "manual") {
        val constraints = defaultConstraints()
        val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("$TAG_IMMEDIATE:$reason")
            .build()
        workManager.enqueueUniqueWork(
            "$TAG_IMMEDIATE-$reason",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun defaultConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    private companion object {
        private const val SYNC_INTERVAL_MINUTES = 15L
        private const val TAG_PERIODIC = "periodic_sync"
        private const val TAG_IMMEDIATE = "sync_now"
    }
}
