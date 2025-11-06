package com.classroom.quizmaster.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.classroom.quizmaster.data.repo.SyncRepo
import com.classroom.quizmaster.data.util.Time
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import com.classroom.quizmaster.agents.impl.SYNC_OUTPUT_COMPLETED_AT

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepo: SyncRepo
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            syncRepo.sync()
            Result.success(
                workDataOf(SYNC_OUTPUT_COMPLETED_AT to Time.now())
            )
        } catch (e: Exception) {
            Timber.e(e, "Sync failed")
            Result.failure()
        }
    }
}
