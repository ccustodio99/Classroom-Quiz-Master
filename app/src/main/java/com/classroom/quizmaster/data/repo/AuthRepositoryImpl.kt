package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.local.dao.TeacherDao
import com.classroom.quizmaster.data.local.entity.TeacherEntity
import com.classroom.quizmaster.data.remote.FirebaseAuthDataSource
import com.classroom.quizmaster.data.remote.FirebaseClassroomDataSource
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.DemoMode
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val classroomDataSource: FirebaseClassroomDataSource,
    private val preferences: AppPreferencesDataSource,
    private val teacherDao: TeacherDao
) : AuthRepository {

    override val authState: Flow<AuthState> = combine(
        authDataSource.authState,
        preferences.featureFlags,
        preferences.lastTeacherId
    ) { state, flags, lastTeacherId ->
        Triple(state, flags, lastTeacherId)
    }
        .flatMapLatest { (state, flags, lastTeacherId) ->
            when {
                state.isAuthenticated && state.role == UserRole.TEACHER -> flow {
                    val resolved = classroomDataSource.fetchTeacherProfile().getOrNull()
                    emit(resolved?.let { state.copy(teacherProfile = it) } ?: state)
                }
                flags.contains(DemoMode.OFFLINE_FLAG) && !lastTeacherId.isNullOrBlank() -> flow {
                    val teacher = teacherDao.get(lastTeacherId)?.toDomain()
                        ?: Teacher(
                            id = lastTeacherId,
                            displayName = DemoMode.TEACHER_NAME,
                            email = DemoMode.TEACHER_EMAIL,
                            createdAt = Clock.System.now()
                        )
                    emit(
                        AuthState(
                            userId = lastTeacherId,
                            displayName = teacher.displayName,
                            email = teacher.email,
                            isAuthenticated = true,
                            role = UserRole.TEACHER,
                            teacherProfile = teacher
                        )
                    )
                }
                else -> flowOf(state)
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

private fun TeacherEntity.toDomain(): Teacher = Teacher(
    id = id,
    displayName = displayName,
    email = email,
    createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt)
)
