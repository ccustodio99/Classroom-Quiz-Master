package com.classroom.quizmaster.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            return
        }
        val pending = goAsync()
        try {
            val appContext = context.applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                BootCompletedReceiverEntryPoint::class.java
            )
            Log.i(TAG, "Boot completed event received; scheduling sync")
            entryPoint.syncScheduler().schedule()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to schedule sync after boot", t)
        } finally {
            pending.finish()
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootCompletedReceiverEntryPoint {
        fun syncScheduler(): SyncScheduler
    }

    private companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
