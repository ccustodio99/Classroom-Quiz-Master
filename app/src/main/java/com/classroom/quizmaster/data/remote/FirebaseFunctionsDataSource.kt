package com.classroom.quizmaster.data.remote

import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@Singleton
class FirebaseFunctionsDataSource @Inject constructor(
    private val firebaseFunctions: FirebaseFunctions
) {

    suspend fun call(
        name: String,
        data: Map<String, Any?> = emptyMap()
    ): Result<Map<String, Any?>> = runCatching {
        val result = firebaseFunctions
            .getHttpsCallable(name)
            .call(data)
            .await()
        @Suppress("UNCHECKED_CAST")
        (result.data as? Map<String, Any?>)?.toMap() ?: emptyMap()
    }.onFailure { Timber.e(it, "Failed to invoke function $name") }
}
