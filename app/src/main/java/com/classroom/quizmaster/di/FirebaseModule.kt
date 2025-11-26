package com.classroom.quizmaster.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseApp(
        @ApplicationContext context: Context
    ): FirebaseApp {
        return FirebaseApp.getApps(context).firstOrNull()
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:unit-test")
                    .setProjectId("quizmaster-local")
                    .setGcmSenderId("000000000000")
                    .setApiKey("FAKE_API_KEY")
                    .build()
            )
    }

    @Provides
    @Singleton
    fun provideAuth(firebaseApp: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)

    @Provides
    @Singleton
    fun provideFirestore(firebaseApp: FirebaseApp): FirebaseFirestore =
        FirebaseFirestore.getInstance(firebaseApp).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder().build()
                )
                .build()
        }

    @Provides
    @Singleton
    fun provideStorage(firebaseApp: FirebaseApp): FirebaseStorage = FirebaseStorage.getInstance(firebaseApp)

    @Provides
    @Singleton
    fun provideFunctions(firebaseApp: FirebaseApp): FirebaseFunctions = FirebaseFunctions.getInstance(firebaseApp)

    @Provides
    @Singleton
    fun provideCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    @Provides
    @Singleton
    fun provideAnalytics(
        @ApplicationContext context: Context,
        firebaseApp: FirebaseApp
    ): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)
}
