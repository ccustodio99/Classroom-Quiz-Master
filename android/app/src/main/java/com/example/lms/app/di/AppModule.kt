package com.example.lms.app.di

import android.content.Context
import androidx.room.Room
import com.example.lms.core.database.LmsDatabase
import com.example.lms.core.database.dao.ClassDao
import com.example.lms.core.database.dao.OutboxDao
import com.example.lms.core.database.dao.UserDao
import com.example.lms.core.network.ClassRemoteDataSource
import com.example.lms.core.network.firebase.FirebaseClassDataSource
import com.example.lms.core.network.live.LiveSignaling
import com.example.lms.core.network.presence.PresenceService
import com.example.lms.core.sync.SyncOrchestrator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.webrtc.PeerConnectionFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LmsDatabase =
        Room.databaseBuilder(context, LmsDatabase::class.java, "classroom-lms.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(database: LmsDatabase): UserDao = database.userDao()

    @Provides
    fun provideClassDao(database: LmsDatabase): ClassDao = database.classDao()

    @Provides
    fun provideOutboxDao(database: LmsDatabase): OutboxDao = database.outboxDao()

    @Provides
    @Singleton
    fun provideClassRemoteDataSource(firestore: FirebaseFirestore): ClassRemoteDataSource =
        FirebaseClassDataSource(firestore)

    @Provides
    @Singleton
    fun providePresenceService(firestore: FirebaseFirestore): PresenceService = PresenceService(firestore)

    @Provides
    @Singleton
    fun provideLiveSignaling(firestore: FirebaseFirestore): LiveSignaling = LiveSignaling(firestore)

    @Provides
    @Singleton
    fun providePeerConnectionFactory(@ApplicationContext context: Context): PeerConnectionFactory {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions(),
        )
        return PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideSyncOrchestrator(
        outboxDao: OutboxDao,
        remoteDataSource: ClassRemoteDataSource,
        @ApplicationScope scope: CoroutineScope,
    ): SyncOrchestrator = SyncOrchestrator(outboxDao, remoteDataSource, scope)
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
