package com.classroom.quizmaster.data.net.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.LinkedHashSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanListener @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val sessions = MutableStateFlow<List<String>>(emptyList())
    private val discoveredSessions = LinkedHashSet<String>()
    private val lock = Any()
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    init {
        start()
    }

    fun sessions(): Flow<List<String>> = sessions

    fun start() {
        synchronized(lock) {
            if (discoveryListener != null) return
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit

                override fun onDiscoveryStopped(serviceType: String) = Unit

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    stop()
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    stop()
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (serviceInfo.serviceType != SERVICE_TYPE) return
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            synchronized(lock) {
                                discoveredSessions.add(serviceInfo.serviceName)
                                sessions.value = discoveredSessions.toList()
                            }
                        }
                    })
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    synchronized(lock) {
                        discoveredSessions.remove(serviceInfo.serviceName)
                        sessions.value = discoveredSessions.toList()
                    }
                }
            }
            runCatching {
                nsdManager.discoverServices(
                    SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    listener
                )
                discoveryListener = listener
            }.onFailure {
                discoveryListener = null
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            discoveryListener?.let { listener ->
                runCatching { nsdManager.stopServiceDiscovery(listener) }
            }
            discoveryListener = null
            discoveredSessions.clear()
            sessions.value = emptyList()
        }
    }

    companion object {
        private const val SERVICE_TYPE = "_quizmaster._tcp."
    }
}
