package com.classroom.quizmaster.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class FirebaseInitializer @Inject constructor(
    private val firebaseAuth: Lazy<FirebaseAuth>,
    private val applicationScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) {

    init {
        // Warm up FirebaseAuth off the main thread so StrictMode never flags it.
        applicationScope.launch(ioDispatcher) {
            runCatching { firebaseAuth.get() }
                .onFailure { Timber.w(it, "Unable to initialize FirebaseAuth on background thread") }
        }
    }
}
