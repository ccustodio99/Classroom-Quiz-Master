package com.classroom.quizmaster.lan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.classroom.quizmaster.BuildConfig
import com.classroom.quizmaster.R
import com.classroom.quizmaster.data.lan.LanHostManager
import com.classroom.quizmaster.data.lan.NsdHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class QuizMasterLanHostService : Service() {

    @Inject lateinit var lanHostManager: LanHostManager
    @Inject lateinit var nsdHelper: NsdHelper
    @Inject lateinit var json: Json

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.notification_hosting))
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val token = intent?.getStringExtra(EXTRA_TOKEN) ?: return START_NOT_STICKY
        val joinCode = intent.getStringExtra(EXTRA_JOIN_CODE) ?: return START_NOT_STICKY
        val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: "QuizMasterHost"
        val requestedPort = intent.getIntExtra(EXTRA_PORT, BuildConfig.LAN_DEFAULT_PORT)
        val port = lanHostManager.start(token, requestedPort)
        nsdHelper.register(serviceName, port, token, joinCode)
        observeAttempts()
        return START_STICKY
    }

    override fun onDestroy() {
        nsdHelper.stopAdvertising()
        lanHostManager.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun observeAttempts() {
        scope.launch {
            lanHostManager.attemptSubmissions.collectLatest {
                Timber.i("Attempt from ${it.uid} for ${it.questionId}")
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_quiz)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "lan_host"
        private const val NOTIFICATION_ID = 99
        const val EXTRA_TOKEN = "token"
        const val EXTRA_SERVICE_NAME = "serviceName"
        const val EXTRA_JOIN_CODE = "joinCode"
        const val EXTRA_PORT = "port"
        const val ACTION_STOP = "com.classroom.quizmaster.ACTION_STOP_HOST"
    }
}
