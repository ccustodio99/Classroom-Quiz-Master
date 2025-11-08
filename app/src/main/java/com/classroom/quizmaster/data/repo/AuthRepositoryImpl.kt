package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.remote.FirebaseAuthDataSource
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override val authState: Flow<AuthState> = authDataSource.authState

    override suspend fun signInWithEmail(email: String, password: String) {
        authDataSource.signInWithEmail(email, password)
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String) {
        authDataSource.signUpWithEmail(email, password, displayName)
    }

    override suspend fun signInWithGoogle(idToken: String) {
        authDataSource.signInWithGoogle(idToken)
    }

    override suspend fun signInAnonymously(nickname: String) {
        authDataSource.signInAnonymously(nickname)
    }

    override suspend fun logout() {
        authDataSource.signOut()
    }
}
