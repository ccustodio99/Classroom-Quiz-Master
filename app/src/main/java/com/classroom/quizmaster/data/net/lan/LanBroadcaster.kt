package com.classroom.quizmaster.data.net.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanBroadcaster @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val lock = Any()
    private var running = false
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var advertisedSession: String? = null

    fun start(sessionId: String) {
        synchronized(lock) {
            if (running && advertisedSession == sessionId) {
                return
            }
            stop()
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "QuizMaster-$sessionId"
                serviceType = SERVICE_TYPE
                port = SERVICE_PORT
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(service: NsdServiceInfo) {
                    advertisedSession = sessionId
                    running = true
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    running = false
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    running = false
                    advertisedSession = null
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    running = false
                    advertisedSession = null
                }
            }

            runCatching {
                nsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    listener
                )
                registrationListener = listener
                running = true
                advertisedSession = sessionId
            }.onFailure {
                running = false
                advertisedSession = null
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            registrationListener?.let { listener ->
                runCatching { nsdManager.unregisterService(listener) }
            }
            registrationListener = null
            advertisedSession = null
            running = false
        }
    }

    fun isRunning(): Boolean = synchronized(lock) { running }

    companion object {
        private const val SERVICE_TYPE = "_quizmaster._tcp."
        private const val SERVICE_PORT = 35555
    }
}
