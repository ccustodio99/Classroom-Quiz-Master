package com.classroom.quizmaster.data.lan

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanNetworkInfo @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {

    @SuppressLint("MissingPermission")
    fun ipv4(): String =
        findFromConnectivityManager()
            ?: findFromLegacyWifi()
            ?: findFromNetworkInterfaces()
            ?: DEFAULT_HOST

    private fun findFromConnectivityManager(): String? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val active = connectivity.activeNetwork ?: return null
        val capabilities = connectivity.getNetworkCapabilities(active) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        val linkAddresses: List<LinkAddress> =
            connectivity.getLinkProperties(active)?.linkAddresses ?: return null
        return linkAddresses
            .firstOrNull { it.address is Inet4Address }
            ?.address
            ?.hostAddress
    }

    @Suppress("DEPRECATION")
    private fun findFromLegacyWifi(): String? {
        val wifiManager = context.getSystemService(WifiManager::class.java) ?: return null
        val wifiInfo = wifiManager.connectionInfo ?: return null
        val ipAddress = wifiInfo.ipAddress
        if (ipAddress == 0) return null
        return java.net.InetAddress.getByAddress(
            byteArrayOf(
                (ipAddress and 0xff).toByte(),
                (ipAddress shr 8 and 0xff).toByte(),
                (ipAddress shr 16 and 0xff).toByte(),
                (ipAddress shr 24 and 0xff).toByte()
            )
        ).hostAddress
    }

    private fun findFromNetworkInterfaces(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

    companion object {
        private const val DEFAULT_HOST = "0.0.0.0"
    }
}
