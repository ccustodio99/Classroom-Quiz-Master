package com.example.lms.core.network.presence

import com.example.lms.core.model.PresenceRecord
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

class PresenceService(
    private val firestore: FirebaseFirestore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val ttlMs: Long = DEFAULT_TTL_MS,
) {
    suspend fun goOnline(classId: String, userId: String) {
        withContext(dispatcher) {
            val timestamp = now()
            doc(classId, userId).set(
                mapOf(
                    FIELD_CLASS_ID to classId,
                    FIELD_USER_ID to userId,
                    FIELD_UPDATED_AT to timestamp,
                    FIELD_EXPIRES_AT to timestamp + ttlMs,
                ),
            ).await()
        }
    }

    suspend fun heartbeat(classId: String, userId: String) = goOnline(classId, userId)

    suspend fun goOffline(classId: String, userId: String) {
        withContext(dispatcher) {
            doc(classId, userId).delete().await()
        }
    }

    suspend fun pruneExpired(classId: String) {
        withContext(dispatcher) {
            val expired = firestore.collection(COLLECTION_PRESENCE)
                .whereEqualTo(FIELD_CLASS_ID, classId)
                .whereLessThan(FIELD_EXPIRES_AT, now())
                .get()
                .await()
            expired.toPresenceRecords().forEach { record ->
                firestore.collection(COLLECTION_PRESENCE)
                    .document("${record.classId}_${record.userId}")
                    .delete()
                    .await()
            }
        }
    }

    fun observePresence(classId: String): Flow<List<PresenceRecord>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_PRESENCE)
            .whereEqualTo(FIELD_CLASS_ID, classId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let { query ->
                    launch { send(query.toPresenceRecords()) }
                }
            }
        awaitClose { registration.remove() }
    }

    private fun doc(classId: String, userId: String) =
        firestore.collection(COLLECTION_PRESENCE).document("${classId}_${userId}")

    private fun QuerySnapshot.toPresenceRecords(): List<PresenceRecord> = documents.mapNotNull { it.toPresenceRecord() }

    private fun DocumentSnapshot.toPresenceRecord(): PresenceRecord? {
        val classId = getString(FIELD_CLASS_ID)
        val userId = getString(FIELD_USER_ID)
        val updatedAt = getLong(FIELD_UPDATED_AT)
        val expiresAt = getLong(FIELD_EXPIRES_AT)
        return if (classId != null && userId != null && updatedAt != null && expiresAt != null) {
            PresenceRecord(classId, userId, updatedAt, expiresAt)
        } else {
            null
        }
    }

    companion object {
        private const val COLLECTION_PRESENCE = "presence"
        private const val FIELD_CLASS_ID = "classId"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_EXPIRES_AT = "expiresAt"
        private const val DEFAULT_TTL_MS = 30_000L
    }
}
