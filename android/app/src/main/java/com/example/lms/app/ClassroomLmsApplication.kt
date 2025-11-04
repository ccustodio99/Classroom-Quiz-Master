package com.example.lms.app

import android.app.Application
import androidx.work.Configuration
import com.example.lms.core.sync.OutboxWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

@HiltAndroidApp
class ClassroomLmsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        OutboxWorker.schedule(this, DEFAULT_ORG)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        const val DEFAULT_ORG = "demo-org"
    }
}

