package com.classroom.quizmaster.data.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import com.classroom.quizmaster.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class NsdClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun discover(): Flow<LanDiscoveryEvent> = callbackFlow {
        val timeoutJob = launch {
            delay(DISCOVERY_TIMEOUT_MS)
            trySend(LanDiscoveryEvent.Timeout)
        }
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                emitError("Discovery failed $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                emitError("Stop discovery failed $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Timber.i("NSD discovery started $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Timber.i("NSD discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                resolve(serviceInfo, timeoutJob)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.w("NSD service lost ${serviceInfo.serviceName}")
            }

            private fun emitError(message: String) {
                Timber.e(message)
                trySend(LanDiscoveryEvent.Error(message))
            }

            private fun resolve(serviceInfo: NsdServiceInfo, timeoutJob: kotlinx.coroutines.Job) {
                val resolver = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        emitError("Resolve failed $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        timeoutJob.cancel()
                        val attributes = serviceInfo.attributes
                        val token = attributes["token"]?.decodeToString().orEmpty()
                        val joinCode = attributes["join"]?.decodeToString().orEmpty()
                        val ts = attributes["ts"]?.decodeToString()?.toLongOrNull() ?: 0L
                        val host = serviceInfo.primaryHostAddress()
                        trySend(
                            LanDiscoveryEvent.ServiceFound(
                                LanServiceDescriptor(
                                    serviceInfo.serviceName,
                                    host,
                                    serviceInfo.port,
                                    token,
                                    joinCode,
                                    timestamp = ts
                                )
                            )
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    nsdManager.resolveService(
                        serviceInfo,
                        ContextCompat.getMainExecutor(context),
                        resolver
                    )
                } else {
                    @Suppress("DEPRECATION")
                    nsdManager.resolveService(serviceInfo, resolver)
                }
            }
        }
        nsdManager.discoverServices(
            BuildConfig.LAN_SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
        awaitClose {
            timeoutJob.cancel()
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        }
    }

    private fun NsdServiceInfo.primaryHostAddress(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hostAddresses.firstOrNull { it is Inet4Address }?.hostAddress.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            host?.hostAddress.orEmpty()
        }
    }

    companion object {
        private const val DISCOVERY_TIMEOUT_MS = 15_000L
    }
}
