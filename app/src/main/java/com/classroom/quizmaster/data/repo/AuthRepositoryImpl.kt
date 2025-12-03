package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.local.dao.TeacherDao
import com.classroom.quizmaster.data.local.dao.StudentDao
import com.classroom.quizmaster.data.local.entity.TeacherEntity
import com.classroom.quizmaster.data.local.entity.StudentEntity
import com.classroom.quizmaster.data.auth.LocalAuthManager
import com.classroom.quizmaster.data.remote.FirebaseAuthDataSource
import com.classroom.quizmaster.data.remote.FirebaseClassroomDataSource
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.DemoMode
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import com.classroom.quizmaster.util.switchMapLatest

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val classroomDataSource: FirebaseClassroomDataSource,
    private val preferences: AppPreferencesDataSource,
    private val teacherDao: TeacherDao,
    private val studentDao: StudentDao
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
            flow {
                val normalized = state.overrideRole(roles)
                val resolved = if (normalized.needsRoleResolution(roles)) {
                    resolveRoleFromRemote(
                        normalized,
                        roles,
                        classroomDataSource,
                        preferences,
                        authDataSource
                    )
                } else {
                    normalized
                }
                when {
                    resolved.isAuthenticated && resolved.isTeacher -> {
                        val profile = resolved.teacherProfile
                            ?: classroomDataSource.fetchTeacherProfile().getOrNull()
                        emit(profile?.let { resolved.copy(teacherProfile = it) } ?: resolved)
                    }
                    (flags.contains(DemoMode.OFFLINE_FLAG) || flags.contains(LocalAuthManager.LOCAL_TEACHER_FLAG)) &&
                        !lastTeacherId.isNullOrBlank() -> {
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
                    else -> emit(resolved)
                }
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

    override suspend fun sendPasswordReset(email: String) {
        authDataSource.sendPasswordReset(email)
    }

    override suspend fun lookupEmailForUsername(username: String): String? {
        return classroomDataSource.findTeacherEmailByUsername(username).getOrNull()
    }

    override suspend fun updateDisplayName(displayName: String) {
        authDataSource.updateDisplayName(displayName)
        val state = authState.first()
        val userId = state.userId ?: return
        val email = state.email ?: authDataSource.currentUserEmail().orEmpty()
        when (state.role) {
            UserRole.TEACHER -> {
                val teacherProfile = state.teacherProfile
                    ?: Teacher(
                        id = userId,
                        displayName = displayName,
                        email = email,
                        createdAt = Clock.System.now()
                    )
                upsertTeacherProfile(teacherProfile.copy(displayName = displayName))
            }
            UserRole.STUDENT -> {
                upsertStudentProfile(
                    Student(
                        id = userId,
                        displayName = displayName,
                        email = email,
                        createdAt = Clock.System.now()
                    )
                )
            }
        }
    }

    override suspend fun upsertTeacherProfile(teacher: Teacher) {
        classroomDataSource.upsertTeacherProfile(teacher)
            .onSuccess { teacherDao.upsert(teacher.toEntity()) }
            .getOrThrow()
    }

    override suspend fun upsertStudentProfile(student: Student) {
        classroomDataSource.upsertStudentProfile(student)
            .onSuccess { studentDao.upsert(student.toEntity()) }
            .getOrThrow()
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

private fun Teacher.toEntity(): TeacherEntity = TeacherEntity(
    id = id,
    displayName = displayName,
    email = email,
    createdAt = createdAt.toEpochMilliseconds()
)

private fun StudentEntity.toDomain(): Student = Student(
    id = id,
    displayName = displayName,
    email = email,
    createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt)
)

private fun Student.toEntity(): StudentEntity = StudentEntity(
    id = id,
    displayName = displayName,
    email = email,
    createdAt = createdAt.toEpochMilliseconds()
)

private data class AuthStateBundle(
    val state: AuthState,
    val flags: Set<String>,
    val lastTeacherId: String?,
    val roles: Map<String, UserRole>
)

private fun AuthState.overrideRole(roleMap: Map<String, UserRole>): AuthState {
    val override = userId?.let { roleMap[it] }
    return when {
        override == UserRole.TEACHER -> copy(role = UserRole.TEACHER, isTeacher = true)
        override == UserRole.STUDENT -> copy(role = UserRole.STUDENT, isTeacher = false)
        isTeacher -> copy(role = UserRole.TEACHER, isTeacher = true)
        else -> this
    }
}

private fun AuthState.needsRoleResolution(roleMap: Map<String, UserRole>): Boolean =
    isAuthenticated && userId != null && roleMap[userId] == null

private suspend fun resolveRoleFromRemote(
    state: AuthState,
    roles: Map<String, UserRole>,
    classroomDataSource: FirebaseClassroomDataSource,
    preferences: AppPreferencesDataSource,
    authDataSource: FirebaseAuthDataSource
): AuthState {
    val userId = state.userId ?: return state
    val cachedRole = roles[userId]
    val teacherResult = classroomDataSource.fetchTeacherProfile()
    val teacher = if (teacherResult.isSuccess) teacherResult.getOrNull() else null
    val studentResult = classroomDataSource.fetchStudentProfile(userId)
    val student = if (studentResult.isSuccess) studentResult.getOrNull() else null

    // 1) Respect explicit cached role overrides and create missing profiles accordingly.
    if (cachedRole == UserRole.TEACHER) {
        if (teacher != null) {
            preferences.setUserRole(userId, UserRole.TEACHER)
            return state.copy(role = UserRole.TEACHER, isTeacher = true, teacherProfile = teacher)
        } else {
            val fallbackTeacher = Teacher(
                id = userId,
                displayName = state.displayName ?: "Teacher",
                email = state.email ?: authDataSource.currentUserEmail().orEmpty(),
                createdAt = Clock.System.now()
            )
            classroomDataSource.upsertTeacherProfile(fallbackTeacher)
                .onSuccess {
                    preferences.setUserRole(userId, UserRole.TEACHER)
                    return state.copy(role = UserRole.TEACHER, isTeacher = true, teacherProfile = fallbackTeacher)
                }
        }
    }
    if (cachedRole == UserRole.STUDENT) {
        if (student != null) {
            preferences.setUserRole(userId, UserRole.STUDENT)
            return state.copy(role = UserRole.STUDENT, isTeacher = false)
        } else {
            val fallbackStudent = Student(
                id = userId,
                displayName = state.displayName ?: "Student",
                email = state.email ?: authDataSource.currentUserEmail().orEmpty(),
                createdAt = Clock.System.now()
            )
            classroomDataSource.upsertStudentProfile(fallbackStudent)
                .onSuccess {
                    preferences.setUserRole(userId, UserRole.STUDENT)
                    return state.copy(role = UserRole.STUDENT, isTeacher = false)
                }
        }
    }

    // 2) No cached override: prefer an explicit teacher profile, then student.
    if (teacher != null) {
        preferences.setUserRole(userId, UserRole.TEACHER)
        return state.copy(role = UserRole.TEACHER, isTeacher = true, teacherProfile = teacher)
    }
    if (student != null) {
        preferences.setUserRole(userId, UserRole.STUDENT)
        return state.copy(role = UserRole.STUDENT, isTeacher = false)
    }

    // 3) No explicit role cached and no profiles found: default to student to satisfy rules.
    if (cachedRole == null) {
        val fallbackStudent = Student(
            id = userId,
            displayName = state.displayName ?: "Student",
            email = state.email ?: authDataSource.currentUserEmail().orEmpty(),
            createdAt = Clock.System.now()
        )
        classroomDataSource.upsertStudentProfile(fallbackStudent)
            .onSuccess {
                preferences.setUserRole(userId, UserRole.STUDENT)
                return state.copy(role = UserRole.STUDENT, isTeacher = false)
            }
    }

    return state
}
