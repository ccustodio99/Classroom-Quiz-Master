package com.acme.lms.data.util

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

suspend inline fun <reified T> DocumentReference.getAs(): T? =
    get().await().toObject(T::class.java)

fun <T : Any> Query.asFlow(mapper: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshots, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshots != null) {
            trySend(snapshots.documents.map(mapper))
        }
    }
    awaitClose { registration.remove() }
}
