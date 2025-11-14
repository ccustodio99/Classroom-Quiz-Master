package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: Lazy<FirebaseAuth>,
    private val ioDispatcher: CoroutineDispatcher
) {

    @Volatile
    private var cachedAuth: FirebaseAuth? = null

    private suspend fun auth(): FirebaseAuth =
        cachedAuth ?: withContext(ioDispatcher) {
            cachedAuth ?: firebaseAuth.get().also { cachedAuth = it }
        }

    val authState: Flow<AuthState> = callbackFlow {
        val instance = auth()
        val listener = FirebaseAuth.AuthStateListener { authSnapshot ->
            trySend(authSnapshot.currentUser.toAuthState())
        }
        instance.addAuthStateListener(listener)
        awaitClose { instance.removeAuthStateListener(listener) }
    }.conflate()

    suspend fun signInWithEmail(email: String, password: String) = withContext(ioDispatcher) {
        auth().signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String) =
        withContext(ioDispatcher) {
            val result = auth().createUserWithEmailAndPassword(email.trim(), password).await()
            result.user?.updateProfile(
                userProfileChangeRequest { this.displayName = displayName }
            )?.await()
        }

    suspend fun signInWithGoogle(idToken: String) = withContext(ioDispatcher) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth().signInWithCredential(credential).await()
    }

    suspend fun signInAnonymously(nickname: String) = withContext(ioDispatcher) {
        val authInstance = auth()
        val current = authInstance.currentUser
        val user = if (current?.isAnonymous == true) {
            current
        } else {
            authInstance.signInAnonymously().await().user
        }
        user?.updateProfile(
            userProfileChangeRequest { displayName = nickname.ifBlank { "Student" } }
        )?.await()
    }

    suspend fun signOut() = withContext(ioDispatcher) {
        auth().signOut()
    }

    suspend fun currentUserId(): String? = auth().currentUser?.uid

    suspend fun isCurrentUserAnonymous(): Boolean = auth().currentUser?.isAnonymous == true

    private fun FirebaseUser?.toAuthState(): AuthState =
        if (this == null) {
            AuthState()
        } else {
            AuthState(
                userId = uid,
                displayName = displayName,
                email = email,
                isAuthenticated = true,
                role = if (isAnonymous) UserRole.STUDENT else UserRole.TEACHER
            )
        }
}
