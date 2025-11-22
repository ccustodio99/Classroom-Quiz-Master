package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.local.dao.TeacherDao
import com.classroom.quizmaster.data.local.entity.TeacherEntity
import com.classroom.quizmaster.data.auth.LocalAuthManager
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import com.classroom.quizmaster.util.switchMapLatest

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
        preferences.lastTeacherId,
        preferences.userRoles
    ) { state, flags, lastTeacherId, roles ->
        AuthStateBundle(state, flags, lastTeacherId, roles)
    }
        .switchMapLatest { (state, flags, lastTeacherId, roles) ->
            val normalized = state.overrideRole(roles)
            when {
                normalized.isAuthenticated && normalized.isTeacher -> flow {
                    val resolved = classroomDataSource.fetchTeacherProfile().getOrNull()
                    emit(resolved?.let { normalized.copy(teacherProfile = it) } ?: normalized)
                }
                (flags.contains(DemoMode.OFFLINE_FLAG) || flags.contains(LocalAuthManager.LOCAL_TEACHER_FLAG)) &&
                    !lastTeacherId.isNullOrBlank() -> flow {
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
                            isTeacher = true,
                            role = UserRole.TEACHER,
                            teacherProfile = teacher
                        )
                    )
                }
                else -> flowOf(normalized)
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

    override suspend fun getTeacher(teacherId: String): Flow<Teacher?> = flow {
        val teacher = teacherDao.get(teacherId)?.toDomain()
        emit(teacher)
    }
}

private fun TeacherEntity.toDomain(): Teacher = Teacher(
    id = id,
    displayName = displayName,
    email = email,
    createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt)
)

private data class AuthStateBundle(
    val state: AuthState,
    val flags: Set<String>,
    val lastTeacherId: String?,
    val roles: Map<String, UserRole>
)

private fun AuthState.overrideRole(roleMap: Map<String, UserRole>): AuthState {
    val override = userId?.let { roleMap[it] }
    return if (override != null && override != role) copy(role = override, isTeacher = override == UserRole.TEACHER) else this
}
