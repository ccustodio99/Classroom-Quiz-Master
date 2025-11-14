package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.remote.FirebaseAuthDataSource
import com.classroom.quizmaster.data.remote.FirebaseClassroomDataSource
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val classroomDataSource: FirebaseClassroomDataSource
) : AuthRepository {

    override val authState: Flow<AuthState> = authDataSource.authState
        .map { state ->
            if (state.isAuthenticated && state.role == UserRole.TEACHER) {
                val resolved = classroomDataSource.fetchTeacherProfile()
                    .getOrNull()
                if (resolved != null) {
                    state.copy(teacherProfile = resolved)
                } else {
                    state
                }
            } else {
                state
            }
        }
        .distinctUntilChanged()

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
