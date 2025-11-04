package com.example.lms.core.network.live

import android.content.Context
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper around WebRTC DataChannel sessions for classroom live activities. Handles peer
 * connection lifecycle, offer/answer creation, ICE candidates, and message publishing as flows.
 */
class WebRtcPeerSession(
    context: Context,
    private val iceServers: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
    ),
    private val dataChannelLabel: String = DEFAULT_DATA_CHANNEL_LABEL,
) {
    enum class Mode { HOST, PARTICIPANT }

    private val peerConnectionFactory: PeerConnectionFactory
    private val eglBase: EglBase = EglBase.create()
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _connectionState = MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState?> = _connectionState.asStateFlow()

    private val _channelState = MutableStateFlow<DataChannel.State?>(null)
    val channelState: StateFlow<DataChannel.State?> = _channelState.asStateFlow()

    private val mediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
    }

    init {
        ensureFactoryInitialized(context.applicationContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    /**
     * Creates the peer connection and optionally a data channel when operating in host mode.
     */
    fun createPeerConnection(mode: Mode): PeerConnection {
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(signalingState: PeerConnection.SignalingState) = Unit

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) = Unit

            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit

            override fun onIceCandidate(candidate: IceCandidate) = Unit

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit

            override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit

            override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit

            override fun onDataChannel(channel: DataChannel) {
                dataChannel = channel.apply { registerObserver() }
            }

            override fun onRenegotiationNeeded() = Unit

            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) = Unit

            override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) = Unit

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                _connectionState.value = newState
            }
        }

        val connection = peerConnectionFactory.createPeerConnection(iceServers, observer)
            ?: error("Unable to create PeerConnection")
        peerConnection = connection
        if (mode == Mode.HOST) {
            dataChannel = connection.createDataChannel(dataChannelLabel, DataChannel.Init()).apply {
                registerObserver()
            }
        }
        return connection
    }

    /**
     * Creates an SDP offer and sets it as the local description.
     */
    suspend fun createOffer(): SessionDescription {
        val connection = requirePeerConnection()
        val offer = connection.createOfferSuspend(mediaConstraints)
        connection.setLocalDescriptionSuspend(offer)
        return offer
    }

    /**
     * Creates an SDP answer and sets it as the local description.
     */
    suspend fun createAnswer(): SessionDescription {
        val connection = requirePeerConnection()
        val answer = connection.createAnswerSuspend(mediaConstraints)
        connection.setLocalDescriptionSuspend(answer)
        return answer
    }

    suspend fun setRemoteDescription(description: SessionDescription) {
        requirePeerConnection().setRemoteDescriptionSuspend(description)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        requirePeerConnection().addIceCandidate(candidate)
    }

    fun sendMessage(message: String): Boolean {
        val channel = dataChannel ?: return false
        val buffer = ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8))
        return channel.send(DataChannel.Buffer(buffer, false))
    }

    fun close() {
        dataChannel?.dispose()
        dataChannel = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        _connectionState.value = null
        _channelState.value = null
    }

    fun release() {
        close()
        peerConnectionFactory.dispose()
        eglBase.release()
    }

    private fun requirePeerConnection(): PeerConnection =
        peerConnection ?: error("PeerConnection has not been created")

    private fun DataChannel.registerObserver() {
        registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) = Unit

            override fun onStateChange() {
                _channelState.value = state()
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = buffer.data
                val bytes = ByteArray(data.remaining())
                data.get(bytes)
                _messages.tryEmit(bytes.decodeToString())
            }
        })
        _channelState.value = state()
    }

    private suspend fun PeerConnection.createOfferSuspend(constraints: MediaConstraints): SessionDescription =
        suspendCancellableCoroutine { cont ->
            createOffer(object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) {
                    cont.resume(description)
                }

                override fun onSetSuccess() = Unit

                override fun onCreateFailure(error: String) {
                    cont.resumeWithException(IllegalStateException("Offer creation failed: $error"))
                }

                override fun onSetFailure(error: String) = Unit
            }, constraints)
        }

    private suspend fun PeerConnection.createAnswerSuspend(constraints: MediaConstraints): SessionDescription =
        suspendCancellableCoroutine { cont ->
            createAnswer(object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) {
                    cont.resume(description)
                }

                override fun onSetSuccess() = Unit

                override fun onCreateFailure(error: String) {
                    cont.resumeWithException(IllegalStateException("Answer creation failed: $error"))
                }

                override fun onSetFailure(error: String) = Unit
            }, constraints)
        }

    private suspend fun PeerConnection.setLocalDescriptionSuspend(description: SessionDescription) =
        suspendCancellableCoroutine<Unit> { cont ->
            setLocalDescription(object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) = Unit

                override fun onSetSuccess() {
                    cont.resume(Unit)
                }

                override fun onCreateFailure(error: String) = Unit

                override fun onSetFailure(error: String) {
                    cont.resumeWithException(IllegalStateException("Local description failed: $error"))
                }
            }, description)
        }

    private suspend fun PeerConnection.setRemoteDescriptionSuspend(description: SessionDescription) =
        suspendCancellableCoroutine<Unit> { cont ->
            setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) = Unit

                override fun onSetSuccess() {
                    cont.resume(Unit)
                }

                override fun onCreateFailure(error: String) = Unit

                override fun onSetFailure(error: String) {
                    cont.resumeWithException(IllegalStateException("Remote description failed: $error"))
                }
            }, description)
        }

    companion object {
        private const val DEFAULT_DATA_CHANNEL_LABEL = "lms-live"
        @Volatile
        private var factoryInitialized = false
        private val initializationLock = Any()

        private fun ensureFactoryInitialized(context: Context) {
            if (!factoryInitialized) {
                synchronized(initializationLock) {
                    if (!factoryInitialized) {
                        val options = PeerConnectionFactory.InitializationOptions.builder(context)
                            .setEnableInternalTracer(false)
                            .createInitializationOptions()
                        PeerConnectionFactory.initialize(options)
                        factoryInitialized = true
                    }
                }
            }
        }
    }
}
