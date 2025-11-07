package com.classroom.quizmaster.data.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.classroom.quizmaster.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class LanServiceDescriptor(
    val serviceName: String,
    val host: String,
    val port: Int,
    val token: String,
    val joinCode: String,
    val timestamp: Long
) {
    val wsUri: String get() = "ws://$host:$port/ws"
    val qrUri: String get() = "$wsUri?token=$token"
}

sealed interface LanDiscoveryEvent {
    data class ServiceFound(val descriptor: LanServiceDescriptor) : LanDiscoveryEvent
    data class Error(val message: String) : LanDiscoveryEvent
    data object Timeout : LanDiscoveryEvent
}

@Singleton
class NsdHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var registrationListener: NsdManager.RegistrationListener? = null

    fun register(serviceName: String, port: Int, token: String, joinCode: String) {
        stopAdvertising()
        val serviceInfo = NsdServiceInfo().apply {
            serviceType = BuildConfig.LAN_SERVICE_TYPE
            this.serviceName = serviceName
            this.port = port
            setAttribute("token", token)
            setAttribute("join", joinCode)
            setAttribute("ts", System.currentTimeMillis().toString())
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Timber.e("NSD registration failed $errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Timber.i("NSD registered: ${serviceInfo.serviceName}")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Timber.i("NSD unregistered ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Timber.e("NSD unregistration failed $errorCode")
            }
        }
        registrationListener = listener
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stopAdvertising() {
        registrationListener?.let {
            runCatching { nsdManager.unregisterService(it) }
            registrationListener = null
        }
    }

    fun discover(): Flow<LanDiscoveryEvent> = callbackFlow {
        val timeoutJob = launch {
            delay(15_000)
            trySend(LanDiscoveryEvent.Timeout)
        }
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                val message = "Discovery failed $errorCode"
                Timber.e(message)
                trySend(LanDiscoveryEvent.Error(message))
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                val message = "Stop discovery failed $errorCode"
                Timber.e(message)
                trySend(LanDiscoveryEvent.Error(message))
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Timber.i("Discovery started $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Timber.i("Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        val message = "Resolve failed $errorCode"
                        Timber.e(message)
                        trySend(LanDiscoveryEvent.Error(message))
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        timeoutJob.cancel()
                        val attributes = serviceInfo.attributes
                        val token = attributes["token"]?.decodeToString().orEmpty()
                        val join = attributes["join"]?.decodeToString().orEmpty()
                        val ts = attributes["ts"]?.decodeToString()?.toLongOrNull() ?: 0L
                        trySend(
                            LanDiscoveryEvent.ServiceFound(
                                LanServiceDescriptor(
                                    serviceInfo.serviceName,
                                    serviceInfo.host.hostAddress ?: "",
                                    serviceInfo.port,
                                    token = token,
                                    joinCode = join,
                                    timestamp = ts
                                )
                            )
                        )
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.w("Service lost ${serviceInfo.serviceName}")
            }
        }
        nsdManager.discoverServices(BuildConfig.LAN_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        awaitClose {
            timeoutJob.cancel()
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }
}
