package com.example.lms.core.network.firebase

import com.example.lms.core.common.runCatchingResult
import com.example.lms.core.model.Class
import com.example.lms.core.model.LmsResult
import com.example.lms.core.model.User
import com.example.lms.core.network.ClassRemoteDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class FirebaseUserDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    suspend fun signInAnonymously(): LmsResult<User> = runCatchingResult {
        val credential = auth.signInAnonymously().await()
        val user = credential.user ?: error("User missing")
        val doc = firestore.collection("users").document(user.uid)
        val snapshot = doc.get().await()
        snapshot.toObject<User>() ?: User(user.uid, "Guest", user.email, com.example.lms.core.model.UserRole.LEARNER, "demo")
    }
}

class FirebaseClassDataSource(
    private val firestore: FirebaseFirestore,
) : ClassRemoteDataSource {
    override suspend fun fetchClasses(org: String): LmsResult<List<Class>> = runCatchingResult {
        firestore.collection("org")
            .document(org)
            .collection("classes")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject<Class>() }
    }
}

