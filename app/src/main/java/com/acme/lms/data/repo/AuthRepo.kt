package com.acme.lms.data.repo

import com.acme.lms.data.model.User
import com.acme.lms.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepo @Inject constructor(
    private val auth: FirebaseAuth
) {

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = auth.currentUser ?: error("Auth failed")
        User(
            id = firebaseUser.uid,
            name = firebaseUser.displayName.orEmpty(),
            email = firebaseUser.email.orEmpty(),
            role = UserRole.LEARNER, // Fixed: Defaulting role to LEARNER for new sign-ins
            org = firebaseUser.photoUrl?.host.orEmpty() // placeholder for org claim
        )
    }
}
