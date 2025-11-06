package com.classroom.quizmaster.data.net.webrtc

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.webrtc.DataChannel
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class WebRtcHost @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var remoteCandidatesListener: ListenerRegistration? = null
    private var answerListener: ListenerRegistration? = null

    suspend fun start(roomId: String) = withContext(Dispatchers.IO) {
        close()

        val factory = WebRtcFactoryProvider.factory(context)
        val rtcConfig = PeerConnection.RTCConfiguration(WebRtcFactoryProvider.iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        val sessionRef = firestore.collection(SESSIONS_COLLECTION).document(roomId)
        clearExistingSession(sessionRef)

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer by defaultObserver() {
            override fun onIceCandidate(candidate: org.webrtc.IceCandidate) {
                sessionRef.collection(HOST_CANDIDATES_COLLECTION)
                    .add(candidate.toMap())
            }
        }) ?: error("Unable to create peerConnection")

        dataChannel = peerConnection?.createDataChannel("quizmaster-$roomId", DataChannel.Init())

        val offer = peerConnection?.createOfferSuspend() ?: error("Failed to create offer")
        peerConnection?.setLocalDescriptionSuspend(offer)

        sessionRef.set(mapOf("offer" to offer.description)).await()

        answerListener = sessionRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            val answer = snapshot.getString("answer") ?: return@addSnapshotListener
            if (peerConnection?.remoteDescription != null) return@addSnapshotListener
            val description = SessionDescription(SessionDescription.Type.ANSWER, answer)
            scope.launch {
                runCatching { peerConnection?.setRemoteDescriptionSuspend(description) }
                    .onFailure { Timber.e(it, "Failed to apply remote description for room $roomId") }
            }
        }

        remoteCandidatesListener = sessionRef.collection(CLIENT_CANDIDATES_COLLECTION)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                val pc = peerConnection ?: return@addSnapshotListener
                snapshots.documentChanges.forEach { change ->
                    val candidate = change.document.data.toIceCandidate() ?: return@forEach
                    pc.addIceCandidate(candidate)
                }
            }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        remoteCandidatesListener?.remove()
        remoteCandidatesListener = null
        answerListener?.remove()
        answerListener = null

        dataChannel?.close()
        dataChannel = null
        peerConnection?.close()
        peerConnection = null
        scope.coroutineContext.cancelChildren()
    }

    private suspend fun clearExistingSession(sessionRef: com.google.firebase.firestore.DocumentReference) {
        sessionRef.collection(HOST_CANDIDATES_COLLECTION).get().await().documents.forEach { it.reference.delete() }
        sessionRef.collection(CLIENT_CANDIDATES_COLLECTION).get().await().documents.forEach { it.reference.delete() }
        sessionRef.delete().await()
    }

    private fun defaultObserver(): PeerConnection.Observer = object : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) = Unit
        override fun onIceCandidate(candidate: org.webrtc.IceCandidate) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out org.webrtc.IceCandidate>) = Unit
        override fun onAddStream(stream: org.webrtc.MediaStream) = Unit
        override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
        override fun onDataChannel(channel: DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: org.webrtc.RtpReceiver, mediaStreams: Array<out org.webrtc.MediaStream>) = Unit
    }

    private suspend fun PeerConnection.createOfferSuspend(): SessionDescription =
        suspendCancellableCoroutine { continuation ->
            createOffer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription) {
                    continuation.resume(desc)
                }

                override fun onCreateFailure(error: String) {
                    continuation.resumeWithException(IllegalStateException(error))
                }

                override fun onSetSuccess() = Unit
                override fun onSetFailure(p0: String?) = Unit
            }, MediaConstraintsFactory.createDefault())
        }

    private suspend fun PeerConnection.setLocalDescriptionSuspend(description: SessionDescription) =
        suspendCancellableCoroutine<Unit> { continuation ->
            setLocalDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    continuation.resume(Unit)
                }

                override fun onSetFailure(error: String) {
                    continuation.resumeWithException(IllegalStateException(error))
                }

                override fun onCreateSuccess(p0: SessionDescription?) = Unit
                override fun onCreateFailure(p0: String?) = Unit
            }, description)
        }

    private suspend fun PeerConnection.setRemoteDescriptionSuspend(description: SessionDescription) =
        suspendCancellableCoroutine<Unit> { continuation ->
            setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    continuation.resume(Unit)
                }

                override fun onSetFailure(error: String) {
                    continuation.resumeWithException(IllegalStateException(error))
                }

                override fun onCreateSuccess(p0: SessionDescription?) = Unit
                override fun onCreateFailure(p0: String?) = Unit
            }, description)
        }

    private fun org.webrtc.IceCandidate.toMap(): Map<String, Any> = mapOf(
        "sdp" to sdp,
        "sdpMid" to (sdpMid ?: ""),
        "sdpMLineIndex" to sdpMLineIndex
    )

    private fun Map<String, Any>.toIceCandidate(): org.webrtc.IceCandidate? {
        val sdp = this["sdp"] as? String ?: return null
        val mid = this["sdpMid"] as? String ?: return null
        val index = (this["sdpMLineIndex"] as? Number)?.toInt() ?: return null
        return org.webrtc.IceCandidate(mid, index, sdp)
    }

    companion object {
        private const val SESSIONS_COLLECTION = "webrtcSessions"
        private const val HOST_CANDIDATES_COLLECTION = "hostCandidates"
        private const val CLIENT_CANDIDATES_COLLECTION = "clientCandidates"
    }
}
