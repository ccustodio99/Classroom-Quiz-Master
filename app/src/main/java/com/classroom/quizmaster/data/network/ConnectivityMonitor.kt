package com.classroom.quizmaster.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context
) {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateStatus(connectivityManager?.getNetworkCapabilities(network))
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateStatus(networkCapabilities)
        }

        override fun onLost(network: Network) {
            _status.value = ConnectivityStatus.offline()
        }

        override fun onUnavailable() {
            _status.value = ConnectivityStatus.offline()
        }
    }

    init {
        connectivityManager?.let { manager ->
            try {
                manager.registerDefaultNetworkCallback(callback)
            } catch (security: SecurityException) {
                Timber.w(security, "Missing permission for network callbacks")
            } catch (error: RuntimeException) {
                Timber.w(error, "Unable to register network callback")
            }
        }
    }

    private fun updateStatus(capabilities: NetworkCapabilities?) {
        _status.value = mapCapabilities(capabilities)
    }

    private fun readStatus(): ConnectivityStatus {
        val capabilities = connectivityManager?.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)
        }
        return mapCapabilities(capabilities)
    }

    private fun mapCapabilities(capabilities: NetworkCapabilities?): ConnectivityStatus {
        if (capabilities == null) return ConnectivityStatus.offline()
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectivityType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectivityType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectivityType.ETHERNET
            else -> ConnectivityType.OTHER
        }
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return ConnectivityStatus(
            type = type,
            hasInternet = hasInternet,
            isValidated = isValidated
        )
    }
}

enum class ConnectivityType { WIFI, CELLULAR, ETHERNET, OTHER, OFFLINE }

data class ConnectivityStatus(
    val type: ConnectivityType,
    val hasInternet: Boolean,
    val isValidated: Boolean
) {
    val isOffline: Boolean
        get() = type == ConnectivityType.OFFLINE || !hasInternet || !isValidated

    companion object {
        fun offline(): ConnectivityStatus = ConnectivityStatus(
            type = ConnectivityType.OFFLINE,
            hasInternet = false,
            isValidated = false
        )
    }
}
