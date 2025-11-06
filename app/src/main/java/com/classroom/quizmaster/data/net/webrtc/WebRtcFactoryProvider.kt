package com.classroom.quizmaster.data.net.webrtc

import android.content.Context
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import java.util.concurrent.atomic.AtomicBoolean

internal object WebRtcFactoryProvider {

    private val initialised = AtomicBoolean(false)
    private var factory: PeerConnectionFactory? = null

    val eglBase: EglBase by lazy { EglBase.create() }

    val iceServers: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    fun factory(context: Context): PeerConnectionFactory {
        if (initialised.get()) {
            return factory ?: error("PeerConnectionFactory not initialised")
        }
        synchronized(this) {
            if (!initialised.get()) {
                val options = PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
                PeerConnectionFactory.initialize(options)
                factory = PeerConnectionFactory.builder()
                    .setOptions(PeerConnectionFactory.Options())
                    .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
                    .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
                    .createPeerConnectionFactory()
                initialised.set(true)
            }
        }
        return factory ?: error("PeerConnectionFactory not initialised")
    }
}
