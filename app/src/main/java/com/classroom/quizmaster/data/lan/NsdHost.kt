package com.classroom.quizmaster.data.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.classroom.quizmaster.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsdHost @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun advertise(serviceName: String, port: Int, token: String, joinCode: String) {
        stop()
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

    fun stop() {
        registrationListener?.let {
            runCatching { nsdManager.unregisterService(it) }
            registrationListener = null
        }
    }
}
