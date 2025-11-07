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
import com.classroom.quizmaster.R
import com.classroom.quizmaster.data.lan.LanDiscoveryEvent
import com.classroom.quizmaster.data.lan.NsdHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NsdDiscoveryService : Service() {

    @Inject lateinit var nsdHelper: NsdHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.notification_discovery))
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADVERTISE -> {
                val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: return START_STICKY
                val port = intent.getIntExtra(EXTRA_PORT, -1)
                val token = intent.getStringExtra(EXTRA_TOKEN) ?: return START_STICKY
                val joinCode = intent.getStringExtra(EXTRA_JOIN_CODE) ?: return START_STICKY
                nsdHelper.register(serviceName, port, token, joinCode)
            }

            ACTION_DISCOVER -> {
                scope.launch {
                    nsdHelper.discover().collectLatest { event ->
                        when (event) {
                            is LanDiscoveryEvent.ServiceFound -> {
                                val descriptor = event.descriptor
                                Timber.i(
                                    "Discovered host ${descriptor.serviceName}@${descriptor.host}:${descriptor.port}"
                                )
                            }

                            is LanDiscoveryEvent.Error -> Timber.w("Discovery error: ${event.message}")
                            LanDiscoveryEvent.Timeout -> Timber.w("Discovery timed out")
                        }
                    }
                }
            }

            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        nsdHelper.stopAdvertising()
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
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
        private const val CHANNEL_ID = "lan_discovery"
        private const val NOTIFICATION_ID = 42
        const val ACTION_ADVERTISE = "com.classroom.quizmaster.ACTION_ADVERTISE"
        const val ACTION_DISCOVER = "com.classroom.quizmaster.ACTION_DISCOVER"
        const val ACTION_STOP = "com.classroom.quizmaster.ACTION_STOP"
        const val EXTRA_SERVICE_NAME = "serviceName"
        const val EXTRA_PORT = "port"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_JOIN_CODE = "joinCode"
    }
}
