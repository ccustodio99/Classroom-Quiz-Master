package com.example.lms.core.network.live

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.text.Charsets

/**
 * Manages LAN discovery and advertisement using Android's NSD/mDNS APIs. Hosts advertise the
 * `_lms._udp` service type and listeners resolve nearby peers for low-latency live sessions.
 */
class LanDiscoveryManager(
    context: Context,
    private val serviceType: String = "_lms._udp",
    private val defaultServiceName: String = DEFAULT_SERVICE_NAME,
) {
    data class LanService(
        val serviceName: String,
        val hostAddress: InetAddress?,
        val port: Int,
        val attributes: Map<String, String>,
    )

    private val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val resolvedServices = ConcurrentHashMap<String, LanService>()

    private val _services = MutableStateFlow<List<LanService>>(emptyList())
    val services: StateFlow<List<LanService>> = _services.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String?) {
            _isDiscovering.value = true
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            _isDiscovering.value = false
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceType == this@LanDiscoveryManager.serviceType) {
                resolveService(serviceInfo)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            resolvedServices.remove(serviceInfo.serviceName)
            _services.update { services -> services.filterNot { it.serviceName == serviceInfo.serviceName } }
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            _isDiscovering.value = false
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            _isDiscovering.value = false
        }
    }

    fun registerService(port: Int, attributes: Map<String, String> = emptyMap()) {
        unregisterService()
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = defaultServiceName
            serviceType = this@LanDiscoveryManager.serviceType
            this.port = port
            attributes.forEach { (key, value) -> setAttribute(key, value) }
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isAdvertising.value = false
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                _isAdvertising.value = true
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                _isAdvertising.value = false
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isAdvertising.value = false
            }
        }
        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (error: IllegalArgumentException) {
            _isAdvertising.value = false
        }
    }

    fun unregisterService() {
        registrationListener?.let {
            runCatching { nsdManager.unregisterService(it) }
        }
        registrationListener = null
        _isAdvertising.value = false
    }

    fun startDiscovery() {
        if (_isDiscovering.value) return
        resolvedServices.clear()
        _services.value = emptyList()
        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (error: IllegalArgumentException) {
            _isDiscovering.value = false
        }
    }

    fun stopDiscovery() {
        if (!_isDiscovering.value) return
        runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        _isDiscovering.value = false
        resolvedServices.clear()
        _services.value = emptyList()
    }

    fun shutdown() {
        stopDiscovery()
        unregisterService()
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(
            serviceInfo,
            object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val service = serviceInfo.toLanService()
                    resolvedServices[service.serviceName] = service
                    _services.value = resolvedServices.values.sortedBy { it.serviceName }
                }
            },
        )
    }

    private fun NsdServiceInfo.toLanService(): LanService {
        val attributesMap = attributes.mapValues { entry ->
            String(entry.value, Charsets.UTF_8)
        }
        return LanService(serviceName, host, port, attributesMap)
    }

    companion object {
        private const val DEFAULT_SERVICE_NAME = "ClassroomLMS"
    }
}
