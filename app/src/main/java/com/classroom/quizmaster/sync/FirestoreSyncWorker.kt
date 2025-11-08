package com.classroom.quizmaster.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.domain.usecase.SyncPendingOpsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import timber.log.Timber

@HiltWorker
class FirestoreSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val syncPendingOpsUseCase: SyncPendingOpsUseCase,
    private val preferences: AppPreferencesDataSource
) : CoroutineWorker(appContext, workerParameters) {

    private val reason: String =
        workerParameters.inputData.getString(KEY_REASON) ?: REASON_UNKNOWN
    private val pendingHint: Int =
        workerParameters.inputData.getInt(KEY_PENDING_COUNT, -1)

    override suspend fun doWork(): Result {
        Timber.d(
            "Starting Firestore sync (reason=%s, attempt=%d, pendingHint=%d)",
            reason,
            runAttemptCount,
            pendingHint
        )
        return try {
            syncPendingOpsUseCase()
            val now = Clock.System.now().toEpochMilliseconds()
            preferences.updateLastSuccessfulSync(now)
            Timber.d(
                "Firestore sync finished successfully (reason=%s, recordedAt=%d)",
                reason,
                now
            )
            Result.success()
        } catch (cancellation: CancellationException) {
            Timber.d(cancellation, "Firestore sync cancelled")
            throw cancellation
        } catch (t: Throwable) {
            Timber.e(
                t,
                "Firestore sync failure (reason=%s, attempt=%d)",
                reason,
                runAttemptCount
            )
            if (shouldFailFast(t) || runAttemptCount >= MAX_RETRY_ATTEMPTS) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private fun shouldFailFast(throwable: Throwable): Boolean =
        throwable is IllegalArgumentException || throwable is IllegalStateException

    companion object {
        const val UNIQUE_NAME = "firestore_sync_worker"
        const val KEY_REASON = "sync_reason"
        const val KEY_PENDING_COUNT = "sync_pending_count"
        const val REASON_MANUAL = "manual"
        const val REASON_PERIODIC = "periodic"
        const val REASON_QUEUE_FLUSH = "queue_flush"
        const val REASON_STARTUP = "startup"
        private const val REASON_UNKNOWN = "unknown"
        private const val MAX_RETRY_ATTEMPTS = 5
    }
}
