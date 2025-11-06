package com.classroom.quizmaster.di

import android.content.Context
import com.classroom.quizmaster.data.net.lan.LanBroadcaster
import com.classroom.quizmaster.data.net.webrtc.WebRtcHost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideLanBroadcaster(
        @ApplicationContext context: Context
    ): LanBroadcaster = LanBroadcaster(context)

    @Provides
    @Singleton
    fun provideWebRtcHost(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ): WebRtcHost = WebRtcHost(context, firestore)
}
