package com.classroom.quizmaster

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.classroom.quizmaster.data.remote.FirebaseAnalyticsLogger
import com.classroom.quizmaster.di.FirebaseInitializer
import com.classroom.quizmaster.sync.SyncScheduler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class QuizMasterApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var crashlytics: FirebaseCrashlytics

    @Inject
    lateinit var analyticsLogger: FirebaseAnalyticsLogger

    @Inject
    lateinit var firebaseInitializer: FirebaseInitializer

    override fun onCreate() {
        super.onCreate()
        initStrictMode()
        configureCrashlytics()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        syncScheduler.schedule()
        analyticsLogger.logEvent("app_open")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO
            )
            .setWorkerFactory(workerFactory)
            .build()

    private fun initStrictMode() {
        if (!BuildConfig.DEBUG) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }

    private fun configureCrashlytics() {
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
