package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser.toAuthState())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.updateProfile(
            userProfileChangeRequest {
                this.displayName = displayName
            }
        )?.await()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
    }

    override suspend fun signInAnonymously(nickname: String) {
        val current = firebaseAuth.currentUser
        val user = if (current?.isAnonymous == true) {
            current
        } else {
            firebaseAuth.signInAnonymously().await().user
        }
        user?.updateProfile(
            userProfileChangeRequest { displayName = nickname.ifBlank { "Student" } }
        )?.await()
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser?.toAuthState(): AuthState =
        if (this == null) {
            AuthState()
        } else {
            AuthState(
                userId = uid,
                displayName = displayName,
                isAuthenticated = true,
                role = if (isAnonymous) UserRole.STUDENT else UserRole.TEACHER
            )
        }
}
