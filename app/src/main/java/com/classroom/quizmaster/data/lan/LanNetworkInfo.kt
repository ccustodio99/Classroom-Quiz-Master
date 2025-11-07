package com.classroom.quizmaster.data.lan

import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.text.format.Formatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanNetworkInfo @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {

    @SuppressLint("MissingPermission")
    fun ipv4(): String = runCatching {
        val wifiManager = context.getSystemService(WifiManager::class.java)
        val ipAddress = wifiManager?.connectionInfo?.ipAddress
        if (ipAddress != null && ipAddress != 0) {
            return@runCatching Formatter.formatIpAddress(ipAddress)
        }
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return@runCatching addr.hostAddress ?: DEFAULT_HOST
                }
            }
        }
        DEFAULT_HOST
    }.getOrElse { DEFAULT_HOST }

    companion object {
        private const val DEFAULT_HOST = "0.0.0.0"
    }
}
