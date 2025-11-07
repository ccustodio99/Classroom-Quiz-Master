package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String, displayName: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun signInAnonymously(nickname: String)
    suspend fun logout()
}
