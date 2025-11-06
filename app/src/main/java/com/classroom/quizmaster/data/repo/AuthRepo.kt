package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.model.User
import com.classroom.quizmaster.data.model.UserRole
import com.classroom.quizmaster.data.util.DEFAULT_ORG_ID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepo @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val usersCollection get() = firestore.collection("users")

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = auth.currentUser ?: error("Auth failed")
        fetchUserProfile(firebaseUser.uid, firebaseUser)
    }.mapFailure { it.mapAuthError(isSignUp = false) }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        org: String
    ): Result<User> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = auth.currentUser ?: error("Sign up failed")
        if (name.isNotBlank()) {
            val profileUpdates = userProfileChangeRequest { displayName = name }
            firebaseUser.updateProfile(profileUpdates).await()
        }
        val resolvedOrg = org.ifBlank { firebaseUser.resolveOrg() }
        val user = User(
            id = firebaseUser.uid,
            name = name.ifBlank { firebaseUser.displayName.orEmpty() },
            email = firebaseUser.email.orEmpty(),
            role = role,
            org = resolvedOrg
        )
        usersCollection.document(firebaseUser.uid).set(user).await()
        user
    }.mapFailure { it.mapAuthError(isSignUp = true) }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun updateProfile(
        name: String,
        org: String,
        role: UserRole? = null
    ): Result<User> = runCatching {
        val firebaseUser = auth.currentUser ?: error("You must be signed in to update your profile.")
        val current = fetchUserProfile(firebaseUser.uid, firebaseUser)
        val trimmedName = name.trim()
        val resolvedName = when {
            trimmedName.isNotEmpty() -> trimmedName
            current.name.isNotBlank() -> current.name
            firebaseUser.displayName?.isNotBlank() == true -> firebaseUser.displayName!!
            else -> firebaseUser.email?.substringBefore("@").orEmpty()
        }
        if (resolvedName.isNotBlank() && resolvedName != firebaseUser.displayName.orEmpty()) {
            val updates = userProfileChangeRequest { displayName = resolvedName }
            firebaseUser.updateProfile(updates).await()
        }
        val trimmedOrg = org.trim()
        val resolvedOrg = when {
            trimmedOrg.isNotEmpty() -> trimmedOrg
            current.org.isNotBlank() -> current.org
            else -> firebaseUser.resolveOrg()
        }
        val updated = current.copy(
            name = resolvedName,
            org = resolvedOrg,
            role = role ?: current.role,
            email = firebaseUser.email.orEmpty()
        )
        usersCollection
            .document(firebaseUser.uid)
            .set(updated, SetOptions.merge())
            .await()
        updated
    }.mapFailure { it.mapFriendlyError("update your profile") }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val firebaseUser = auth.currentUser ?: error("You must be signed in to delete your account.")
        val userId = firebaseUser.uid
        firebaseUser.delete().await()
        usersCollection.document(userId).delete().await()
        auth.signOut()
    }.mapFailure { it.mapFriendlyError("delete your account") }

    fun observeAuthState(): Flow<User?> = callbackFlow {
        var profileListener: ListenerRegistration? = null
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            profileListener?.remove()
            profileListener = null
            if (firebaseUser == null) {
                trySend(null).isSuccess
            } else {
                val docRef = usersCollection.document(firebaseUser.uid)
                profileListener = docRef.addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val parsed = snapshot?.toObject(User::class.java)
                    val user = parsed?.copy(
                        id = firebaseUser.uid,
                        email = parsed.email.ifBlank { firebaseUser.email.orEmpty() }
                    )
                    if (user != null) {
                        trySend(user).isSuccess
                    }
                }
                launch {
                    val profile = runCatching { fetchUserProfile(firebaseUser.uid, firebaseUser) }
                        .getOrElse {
                            User(
                                id = firebaseUser.uid,
                                name = firebaseUser.displayName.orEmpty(),
                                email = firebaseUser.email.orEmpty(),
                                role = UserRole.LEARNER,
                                org = firebaseUser.resolveOrg()
                            )
                        }
                    trySend(profile).isSuccess
                }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            profileListener?.remove()
            auth.removeAuthStateListener(listener)
        }
    }

    private suspend fun fetchUserProfile(uid: String, firebaseUser: FirebaseUser? = null): User {
        val snapshot = usersCollection.document(uid).get().await()
        val cached = snapshot.toObject(User::class.java)
        if (cached != null) {
            return cached.copy(
                id = uid,
                email = cached.email.ifBlank { firebaseUser?.email.orEmpty() }
            )
        }
        val fbUser = firebaseUser ?: auth.currentUser ?: error("User $uid is not signed in")
        val fallback = User(
            id = fbUser.uid,
            name = fbUser.displayName.orEmpty(),
            email = fbUser.email.orEmpty(),
            role = UserRole.LEARNER,
            org = fbUser.resolveOrg()
        )
        usersCollection.document(uid).set(fallback).await()
        return fallback
    }

    private fun FirebaseUser.resolveOrg(): String {
        val tenant = tenantId?.takeIf { it.isNotBlank() }
        if (!tenant.isNullOrBlank()) return tenant

        val emailDomain = email
            ?.substringAfter("@", missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
        if (!emailDomain.isNullOrBlank()) return emailDomain.lowercase()

        val photoHost = photoUrl?.host?.takeIf { it.isNotBlank() }
        if (!photoHost.isNullOrBlank()) return photoHost.lowercase()

        return DEFAULT_ORG_ID
    }
}

private fun Throwable.mapAuthError(isSignUp: Boolean): Throwable {
    val action = if (isSignUp) "create account" else "sign in"
    val authException = findFirebaseAuthException()
    val friendlyMessage = when {
        authException?.errorCode.equals("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
            "Email/password sign-in is disabled for this project. Ask your administrator to enable the Email/Password provider in Firebase Authentication."
        authException?.errorCode.equals("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) ->
            "An account already exists for that email address."
        authException?.errorCode.equals("ERROR_INVALID_EMAIL", ignoreCase = true) ->
            "Enter a valid email address."
        authException?.errorCode.equals("ERROR_WEAK_PASSWORD", ignoreCase = true) ->
            "Password is too weak. Please choose a stronger password."
        else -> this.message ?: "Unable to $action at the moment. Please try again."
    }
    return IllegalStateException(friendlyMessage, this)
}

private fun Throwable.mapFriendlyError(action: String): Throwable {
    val authException = findFirebaseAuthException()
    val requiresRecentLogin =
        this is FirebaseAuthRecentLoginRequiredException ||
            authException?.errorCode.equals("ERROR_REQUIRES_RECENT_LOGIN", ignoreCase = true)
    val friendlyMessage = when {
        requiresRecentLogin -> "Please sign in again to $action."
        authException?.errorCode.equals("ERROR_USER_NOT_FOUND", ignoreCase = true) ->
            "We couldn't find your account. Try signing in again."
        else -> this.message ?: "Unable to $action right now. Please try again."
    }
    return IllegalStateException(friendlyMessage, this)
}

private tailrec fun Throwable?.findFirebaseAuthException(): FirebaseAuthException? = when (this) {
    null -> null
    is FirebaseAuthException -> this
    else -> this.cause.findFirebaseAuthException()
}

private inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
    fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(transform(it)) })
