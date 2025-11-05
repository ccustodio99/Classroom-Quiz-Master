package com.acme.lms.agents.impl

import com.acme.lms.agents.PresenceAgent
import com.acme.lms.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceAgentImpl @Inject constructor() : PresenceAgent {

    private val db = Firebase.database

    override fun goOnline(classId: String, userId: String) {
        val presenceRef = db.getReference("presence").child(classId).child(userId)
        presenceRef.setValue(true)
        presenceRef.onDisconnect().removeValue()
    }

    override fun goOffline(classId: String, userId: String) {
        db.getReference("presence").child(classId).child(userId).removeValue()
    }

    override fun getOnlineUsers(classId: String): Flow<List<User>> = callbackFlow {
        val presenceRef = db.getReference("presence").child(classId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull {
                    it.key?.let { User(id = it) }
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        presenceRef.addValueEventListener(listener)
        awaitClose { presenceRef.removeEventListener(listener) }
    }
}
