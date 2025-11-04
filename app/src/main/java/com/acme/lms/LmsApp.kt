package com.acme.lms

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LmsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.firestore.apply {
            firestoreSettings = firestoreSettings {
                isPersistenceEnabled = true
                cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
            }
        }
    }
}
