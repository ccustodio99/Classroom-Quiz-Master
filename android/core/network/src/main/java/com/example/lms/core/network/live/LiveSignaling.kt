package com.example.lms.core.network.live

import com.example.lms.core.model.LiveSignalMessage
import com.example.lms.core.model.SignalType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LiveSignaling(
    private val firestore: FirebaseFirestore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun publish(message: LiveSignalMessage) {
        withContext(dispatcher) {
            firestore.collection(ROOT_COLLECTION)
                .document(message.sessionId)
                .collection(message.type.collectionName)
                .add(message.toFirestoreMap())
                .await()
        }
    }

    fun listen(sessionId: String, type: SignalType): Flow<List<LiveSignalMessage>> = callbackFlow {
        val registration = firestore.collection(ROOT_COLLECTION)
            .document(sessionId)
            .collection(type.collectionName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let { query ->
                    launch { send(query.toMessages(sessionId, type)) }
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun clearSession(sessionId: String) {
        withContext(dispatcher) {
            val sessionDoc = firestore.collection(ROOT_COLLECTION).document(sessionId)
            val snapshot = sessionDoc.collection(SignalType.OFFER.collectionName).get().await()
            snapshot.documents.forEach { it.reference.delete().await() }
            sessionDoc.collection(SignalType.ANSWER.collectionName).get().await().documents.forEach {
                it.reference.delete().await()
            }
            sessionDoc.collection(SignalType.ICE.collectionName).get().await().documents.forEach {
                it.reference.delete().await()
            }
        }
    }

    private fun LiveSignalMessage.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_PEER_ID to peerId,
        FIELD_TYPE to type.name.lowercase(),
        FIELD_SDP to sdp,
        FIELD_CANDIDATE to candidate,
        FIELD_SDP_MID to sdpMid,
        FIELD_SDP_MLINE to sdpMLineIndex,
        FIELD_TIMESTAMP to timestamp,
    )

    private fun QuerySnapshot.toMessages(sessionId: String, type: SignalType): List<LiveSignalMessage> = documents.mapNotNull {
        it.toMessage(sessionId, type)
    }

    private fun DocumentSnapshot.toMessage(sessionId: String, type: SignalType): LiveSignalMessage? {
        val peerId = getString(FIELD_PEER_ID) ?: return null
        val timestamp = getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis()
        val sdp = getString(FIELD_SDP)
        val candidate = getString(FIELD_CANDIDATE)
        val sdpMid = getString(FIELD_SDP_MID)
        val sdpMLineIndex = getLong(FIELD_SDP_MLINE)?.toInt()
        return LiveSignalMessage(
            sessionId = sessionId,
            peerId = peerId,
            type = type,
            sdp = sdp,
            candidate = candidate,
            sdpMid = sdpMid,
            sdpMLineIndex = sdpMLineIndex,
            timestamp = timestamp,
        )
    }

    private val SignalType.collectionName: String
        get() = when (this) {
            SignalType.OFFER -> "offers"
            SignalType.ANSWER -> "answers"
            SignalType.ICE -> "ice"
        }

    companion object {
        private const val ROOT_COLLECTION = "live"
        private const val FIELD_PEER_ID = "peerId"
        private const val FIELD_TYPE = "type"
        private const val FIELD_SDP = "sdp"
        private const val FIELD_CANDIDATE = "candidate"
        private const val FIELD_SDP_MID = "sdpMid"
        private const val FIELD_SDP_MLINE = "sdpMLineIndex"
        private const val FIELD_TIMESTAMP = "timestamp"
    }
}
