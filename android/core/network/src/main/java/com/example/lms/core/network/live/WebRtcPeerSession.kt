package com.example.lms.core.network.live

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.webrtc.DataChannel
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import java.nio.ByteBuffer

class WebRtcPeerSession private constructor(
    private val peerConnection: PeerConnection,
    private val dataChannel: DataChannel,
) {
    private val incoming = MutableSharedFlow<String>(extraBufferCapacity = 16)

    val messages: SharedFlow<String> = incoming.asSharedFlow()

    init {
        dataChannel.registerObserver(
            object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) = Unit

                override fun onStateChange() = Unit

                override fun onMessage(buffer: DataChannel.Buffer) {
                    val bytes = ByteArray(buffer.data.remaining())
                    buffer.data.get(bytes)
                    incoming.tryEmit(String(bytes))
                }
            },
        )
    }

    fun sendText(message: String) {
        val buffer = ByteBuffer.wrap(message.toByteArray())
        dataChannel.send(DataChannel.Buffer(buffer, false))
    }

    fun close() {
        dataChannel.close()
        peerConnection.close()
    }

    companion object {
        fun create(
            factory: PeerConnectionFactory,
            iceServers: List<PeerConnection.IceServer> = emptyList(),
            label: String = "lms-channel",
        ): WebRtcPeerSession {
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
            val connection = factory.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: org.webrtc.IceCandidate?) = Unit
                    override fun onDataChannel(dc: DataChannel?) = Unit
                    override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
                    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) = Unit
                    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
                    override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
                    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) = Unit
                    override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) = Unit
                    override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit
                    override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
                    override fun onRenegotiationNeeded() = Unit
                    override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) = Unit
                },
            ) ?: throw IllegalStateException("PeerConnection creation failed")
            val init = DataChannel.Init()
            val channel = connection.createDataChannel(label, init)
            return WebRtcPeerSession(connection, channel)
        }
    }
}
