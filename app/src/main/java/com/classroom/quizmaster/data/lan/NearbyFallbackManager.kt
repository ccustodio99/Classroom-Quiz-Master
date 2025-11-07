package com.classroom.quizmaster.data.lan

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Placeholder for future Wi-Fi Direct / Nearby integrations.
 * Currently surfaces a descriptive error so the UI can point users back to QR/manual join.
 */
@Singleton
class NearbyFallbackManager @Inject constructor() {

    fun advertise(serviceName: String, port: Int) {
        Timber.d("Nearby fallback advertising stub invoked for %s:%d", serviceName, port)
    }

    fun discover(): Flow<LanDiscoveryEvent> = flow {
        emit(
            LanDiscoveryEvent.Error(
                "Nearby fallback not yet available on this device; please scan the QR or type the ws:// URL."
            )
        )
    }
}
