package com.classroom.quizmaster.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.domain.usecase.SyncPendingOpsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import timber.log.Timber

@HiltWorker
class FirestoreSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val syncPendingOpsUseCase: SyncPendingOpsUseCase,
    private val preferences: AppPreferencesDataSource
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result = try {
        syncPendingOpsUseCase()
        preferences.updateLastSuccessfulSync(Clock.System.now().toEpochMilliseconds())
        Result.success()
    } catch (t: Throwable) {
        Timber.e(t, "Worker failure")
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "firestore_sync_worker"
    }
}
