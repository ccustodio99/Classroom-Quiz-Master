package com.classroom.quizmaster.data.lan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.classroom.quizmaster.BuildConfig
import com.classroom.quizmaster.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LanHostForegroundService : Service() {

    @Inject lateinit var lanHostServer: LanHostServer
    @Inject lateinit var nsdHost: NsdHost
    @Inject lateinit var lanNetworkInfo: LanNetworkInfo

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notification_hosting)))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_REFRESH -> handleRefresh()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        nsdHost.stop()
        lanHostServer.stop()
        super.onDestroy()
    }

    private fun handleStart(intent: Intent) {
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return
        val joinCode = intent.getStringExtra(EXTRA_JOIN_CODE) ?: return
        val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: "QuizMaster"
        val teacherName = intent.getStringExtra(EXTRA_TEACHER_NAME)
        val requestedPort = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)
        scope.launch {
            val boundPort = lanHostServer.start(token, requestedPort)
            nsdHost.advertise(serviceName, boundPort, token, joinCode, teacherName)
            updateNotification(boundPort)
        }
    }

    private fun handleRefresh() {
        scope.launch {
            val port = lanHostServer.activePort ?: DEFAULT_PORT
            updateNotification(port)
        }
    }

    private fun updateNotification(port: Int) {
        val host = lanNetworkInfo.ipv4()
        val text = getString(R.string.notification_hosting)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("$text · $host:$port")
            .setSmallIcon(R.drawable.ic_stat_quiz)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_quiz)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_hosting)
                setShowBadge(false)
            }
            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "lan_host_channel"
        private const val NOTIFICATION_ID = 0x50414e
        private const val DEFAULT_PORT = BuildConfig.LAN_DEFAULT_PORT
        const val ACTION_START = "com.classroom.quizmaster.action.START_LAN_HOST"
        const val ACTION_REFRESH = "com.classroom.quizmaster.action.REFRESH_LAN_HOST"
        const val ACTION_STOP = "com.classroom.quizmaster.action.STOP_LAN_HOST"
        const val EXTRA_TOKEN = "extra_token"
        const val EXTRA_SERVICE_NAME = "extra_service_name"
        const val EXTRA_JOIN_CODE = "extra_join_code"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_TEACHER_NAME = "extra_teacher_name"

        fun start(
            context: Context,
            token: String,
            joinCode: String,
            serviceName: String,
            port: Int,
            teacherName: String
        ) {
            val intent = Intent(context, LanHostForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_JOIN_CODE, joinCode)
                putExtra(EXTRA_SERVICE_NAME, serviceName)
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_TEACHER_NAME, teacherName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LanHostForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
