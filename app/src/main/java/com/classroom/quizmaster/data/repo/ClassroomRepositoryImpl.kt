package com.classroom.quizmaster.data.repo

import androidx.room.withTransaction
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.dao.ClassroomDao
import com.classroom.quizmaster.data.local.dao.TopicDao
import com.classroom.quizmaster.data.local.dao.StudentDao
import com.classroom.quizmaster.data.local.dao.JoinRequestDao
import com.classroom.quizmaster.data.local.entity.ClassroomEntity
import com.classroom.quizmaster.data.local.entity.TopicEntity
import com.classroom.quizmaster.data.local.entity.StudentEntity
import com.classroom.quizmaster.data.local.entity.JoinRequestEntity
import com.classroom.quizmaster.data.remote.FirebaseClassroomDataSource
import com.classroom.quizmaster.data.remote.FirebaseTopicDataSource
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.JoinRequest
import com.classroom.quizmaster.domain.model.JoinRequestStatus
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import com.classroom.quizmaster.util.switchMapLatest

@Singleton
class ClassroomRepositoryImpl @Inject constructor(
    private val database: QuizMasterDatabase,
    private val classroomDao: ClassroomDao,
    private val topicDao: TopicDao,
    private val studentDao: StudentDao,
    private val joinRequestDao: JoinRequestDao,
    private val classroomRemote: FirebaseClassroomDataSource,
    private val topicRemote: FirebaseTopicDataSource,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ClassroomRepository {

    override val classrooms: Flow<List<Classroom>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val userId = auth.userId
                if (userId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    if (auth.isTeacher) {
                        classroomDao.observeForTeacher(userId)
                    } else {
                        classroomDao.observeForStudent(userId)
                    }
                        .map { entities ->
                            entities
                                .filterNot { it.isArchived }
                                .map { it.toDomain() }
                        }
                }
            }
            .distinctUntilChanged()

    override val archivedClassrooms: Flow<List<Classroom>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val teacherId = auth.userId
                if (teacherId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    classroomDao.observeForTeacher(teacherId)
                        .map { entities ->
                            entities.filter { it.isArchived }.map { it.toDomain() }
                        }
                }
            }
            .distinctUntilChanged()

    override val topics: Flow<List<Topic>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val userId = auth.userId
                if (userId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        if (auth.isTeacher) {
                            classroomDao.observeForTeacher(userId)
                        } else {
                            classroomDao.observeForStudent(userId)
                        },
                        topicDao.observeForTeacher(userId)
                    ) { classrooms, topics ->
                        val activeClassroomIds = classrooms.filterNot { it.isArchived }.map { it.id }.toSet()
                        topics
                            .filter { topic -> !topic.isArchived && topic.classroomId in activeClassroomIds }
                            .map { it.toDomain() }
                    }
                }
            }
            .distinctUntilChanged()

    override val archivedTopics: Flow<List<Topic>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val teacherId = auth.userId
                if (teacherId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    topicDao.observeForTeacher(teacherId)
                        .map { entities ->
                            entities.filter { it.isArchived }.map { it.toDomain() }
                        }
                }
            }
            .distinctUntilChanged()

     override val joinRequests: Flow<List<JoinRequest>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val teacherId = auth.userId
                if (teacherId.isNullOrBlank() || !auth.isTeacher) {
                    flowOf(emptyList())
                } else {
                    joinRequestDao.observeForTeacher(teacherId)
                        .map { entities ->
                            entities.map { it.toDomain() }
                        }
                }
            }
            .distinctUntilChanged()


    override suspend fun refresh() = withContext(ioDispatcher) {
        val authState = authRepository.authState.firstOrNull() ?: return@withContext
        if (authState.isTeacher) {
            val classrooms = classroomRemote.fetchClassrooms().getOrElse { emptyList() }
            val topics = topicRemote.fetchTopics().getOrElse { emptyList() }
            val joinRequests = authState.userId?.let { teacherId ->
                classroomRemote.fetchJoinRequestsForTeacher(teacherId).getOrElse { emptyList() }
            }.orEmpty()
            val students = joinRequests.map { it.studentId }.distinct().mapNotNull { id ->
                classroomRemote.fetchStudentProfile(id).getOrNull()
            }
            if (classrooms.isEmpty() && topics.isEmpty() && joinRequests.isEmpty() && students.isEmpty()) return@withContext
            database.withTransaction {
                if (classrooms.isNotEmpty()) {
                    classroomDao.upsertAll(classrooms.map { it.toEntity() })
                }
                if (topics.isNotEmpty()) {
                    topicDao.upsertAll(topics.map { it.toEntity() })
                }
                if (joinRequests.isNotEmpty()) {
                    joinRequestDao.upsertAll(joinRequests.map { it.toEntity() })
                }
                if (students.isNotEmpty()) {
                    studentDao.upsertAll(students.map { it.toEntity() })
                }
            }
        } else {
            val studentId = authState.userId ?: return@withContext
            val classrooms = classroomRemote.getClassroomsForStudent(studentId).getOrElse { emptyList() }
            if (classrooms.isEmpty()) return@withContext
            database.withTransaction {
                classroomDao.upsertAll(classrooms.map { it.toEntity() })
            }
        }
    }

    override suspend fun upsertClassroom(classroom: Classroom): String = withContext(ioDispatcher) {
        val teacherId = requireTeacherId()
        val now = Clock.System.now()
        val resolvedId = classroom.id.ifBlank { generateLocalId("classroom") }
        val createdAt = if (classroom.createdAt > Instant.DISTANT_PAST) classroom.createdAt else now
        val normalized = classroom.copy(
            id = resolvedId,
            teacherId = teacherId,
            createdAt = createdAt,
            updatedAt = now,
            archivedAt = if (classroom.isArchived) classroom.archivedAt ?: now else null
        )
        database.withTransaction { classroomDao.upsert(normalized.toEntity()) }
        val remoteResult = classroomRemote.upsertClassroom(normalized)
        val remoteId = remoteResult.getOrElse { error ->
            if (shouldIgnorePermissionDenied(error)) {
                Timber.w(error, "Skipping remote classroom upsert due to permissions")
                resolvedId
            } else {
                throw error
            }
        }
        if (remoteId != resolvedId) {
            val reconciled = normalized.copy(id = remoteId)
            database.withTransaction { classroomDao.upsert(reconciled.toEntity()) }
            remoteId
        } else {
            resolvedId
        }
    }

    override suspend fun createJoinRequest(joinCode: String) = withContext(ioDispatcher) {
        val studentId = requireStudentId()
        val classroom = classroomRemote.getClassroomByJoinCode(joinCode).getOrThrow()
        if (classroom.isArchived) {
            throw IllegalStateException("Cannot join an archived classroom.")
        }

        val joinRequest = JoinRequest(
            id = "",
            studentId = studentId,
            classroomId = classroom.id,
            teacherId = classroom.teacherId,
            status = JoinRequestStatus.PENDING,
            createdAt = Clock.System.now()
        )
        val remoteId = classroomRemote.createJoinRequest(joinRequest).getOrThrow()
        joinRequestDao.upsert(joinRequest.copy(id = remoteId).toEntity())
    }

    override suspend fun createJoinRequest(classroomId: String, teacherId: String) = withContext(ioDispatcher) {
        val studentId = requireStudentId()

        val joinRequest = JoinRequest(
            id = "",
            studentId = studentId,
            classroomId = classroomId,
            teacherId = teacherId,
            status = JoinRequestStatus.PENDING,
            createdAt = Clock.System.now()
        )
        val remoteId = classroomRemote.createJoinRequest(joinRequest).getOrThrow()
        joinRequestDao.upsert(joinRequest.copy(id = remoteId).toEntity())
    }

    override suspend fun approveJoinRequest(requestId: String) = withContext(ioDispatcher) {
        val teacherId = requireTeacherId()
        val request = joinRequestDao.get(requestId) ?: throw IllegalArgumentException("Join request not found")
        check(request.teacherId == teacherId) { "Cannot approve another teacher's join request" }

        classroomRemote.approveJoinRequest(requestId).getOrThrow()

        val updatedRequest = request.copy(
            status = JoinRequestStatus.APPROVED.name,
            resolvedAt = Clock.System.now().toEpochMilliseconds()
        )
        val classroom = classroomDao.get(request.classroomId)
        if (classroom != null) {
            val updatedStudents = (classroom.students + request.studentId).distinct()
            classroomDao.upsert(classroom.copy(students = updatedStudents))
        }
        joinRequestDao.upsert(updatedRequest)
    }

    override suspend fun denyJoinRequest(requestId: String) = withContext(ioDispatcher) {
        val teacherId = requireTeacherId()
        val request = joinRequestDao.get(requestId) ?: throw IllegalArgumentException("Join request not found")
        check(request.teacherId == teacherId) { "Cannot deny another teacher's join request" }

        classroomRemote.denyJoinRequest(requestId).getOrThrow()

        val updatedRequest = request.copy(
            status = JoinRequestStatus.DENIED.name,
            resolvedAt = Clock.System.now().toEpochMilliseconds()
        )
        joinRequestDao.upsert(updatedRequest)
    }


    override suspend fun archiveClassroom(classroomId: String, archivedAt: Instant) = withContext(ioDispatcher) {
        val teacherId = requireTeacherId()
        val existing = classroomDao.get(classroomId) ?: return@withContext
        check(existing.teacherId == teacherId) { "Cannot archive another teacher's classroom" }
        val archived = existing.copy(
            updatedAt = archivedAt.toEpochMilliseconds(),
            isArchived = true,
            archivedAt = archivedAt.toEpochMilliseconds()
        )
        database.withTransaction { classroomDao.upsert(archived) }
        classroomRemote.archiveClassroom(classroomId, archivedAt)
            .onFailure { error ->
                if (!shouldIgnorePermissionDenied(error) && !isTransient(error)) throw error else Timber.w(error, "Skipping remote archive for $classroomId")
            }
            .getOrElse { }
    }

    override suspend fun upsertTopic(topic: Topic): String = withContext(ioDispatcher) {
        val teacherId = requireTeacherId()
        val now = Clock.System.now()
        val resolvedId = topic.id.ifBlank { generateLocalId("topic") }
        val createdAt = if (topic.createdAt > Instant.DISTANT_PAST) topic.createdAt else now
        val parent = classroomDao.get(topic.classroomId)
            ?: error("Parent classroom ${topic.classroomId} not found")
        check(parent.teacherId == teacherId) { "Cannot attach a topic to another teacher's classroom" }
        check(!parent.isArchived) { "Cannot attach a topic to an archived classroom" }
        val normalized = topic.copy(
            id = resolvedId,
            classroomId = parent.id,
            teacherId = teacherId,
            updatedAt = now,
            archivedAt = if (topic.isArchived) topic.archivedAt ?: now else null,
            createdAt = createdAt
        )
        database.withTransaction { topicDao.upsert(normalized.toEntity()) }
        val remoteResult = topicRemote.upsertTopic(normalized)
        val remoteId = remoteResult.getOrElse { error ->
            if (shouldIgnorePermissionDenied(error)) {
                Timber.w(error, "Skipping remote topic upsert due to permissions")
                resolvedId
            } else {
                throw error
            }
        }
        if (remoteId != resolvedId) {
            val reconciled = normalized.copy(id = remoteId)
            database.withTransaction { topicDao.upsert(reconciled.toEntity()) }
            remoteId
        } else {
            resolvedId
        }
    }

    override suspend fun archiveTopic(topicId: String, archivedAt: Instant) = withContext(ioDispatcher) {
        val teacherId = requireTeacherId()
        val existing = topicDao.get(topicId) ?: return@withContext
        check(existing.teacherId == teacherId) { "Cannot archive another teacher's topic" }
        val archived = existing.copy(
            updatedAt = archivedAt.toEpochMilliseconds(),
            isArchived = true,
            archivedAt = archivedAt.toEpochMilliseconds()
        )
        database.withTransaction { topicDao.upsert(archived) }
        topicRemote.archiveTopic(topicId, archivedAt)
            .onFailure { error ->
                if (!shouldIgnorePermissionDenied(error) && !isTransient(error)) throw error else Timber.w(error, "Skipping remote topic archive for $topicId")
            }
            .getOrElse { }
    }

    override suspend fun getStudent(id: String): Student? = withContext(ioDispatcher) {
        studentDao.get(id)?.toDomain()
    }

    override suspend fun searchTeachers(query: String): List<Teacher> = withContext(ioDispatcher) {
        classroomRemote.searchTeachers(query).getOrThrow()
    }

    override suspend fun getClassroomsForTeacher(teacherId: String): List<Classroom> = withContext(ioDispatcher) {
        classroomRemote.getClassroomsForTeacher(teacherId).getOrThrow()
    }

    override suspend fun getClassroom(id: String): Classroom? = withContext(ioDispatcher) {
        val userId = currentUserId() ?: return@withContext null
        val authState = authRepository.authState.firstOrNull() ?: return@withContext null
        val classroom = classroomDao.get(id)
            ?.takeUnless { it.isArchived }
            ?: return@withContext null

        if (authState.isTeacher) {
            if (classroom.teacherId != userId) return@withContext null
        } else {
            if (userId !in classroom.students) return@withContext null
        }

        classroom.toDomain()
    }

    override suspend fun getTopic(id: String): Topic? = withContext(ioDispatcher) {
        val userId = currentUserId() ?: return@withContext null
        val entity = topicDao.get(id)
            ?.takeUnless { it.isArchived || it.teacherId != userId }
            ?: return@withContext null
        classroomDao.get(entity.classroomId)
            ?.takeUnless { it.isArchived || it.teacherId != userId }
            ?: return@withContext null
        entity.toDomain()
    }

    private fun Classroom.toEntity(): ClassroomEntity = ClassroomEntity(
        id = id.ifBlank { generateLocalId("classroom") },
        teacherId = teacherId,
        name = name,
        grade = grade,
        subject = subject,
        joinCode = joinCode,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        isArchived = isArchived,
        archivedAt = archivedAt?.toEpochMilliseconds(),
        students = students
    )

    private fun Topic.toEntity(): TopicEntity = TopicEntity(
        id = id.ifBlank { generateLocalId("topic") },
        classroomId = classroomId,
        teacherId = teacherId,
        name = name,
        description = description,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        isArchived = isArchived,
        archivedAt = archivedAt?.toEpochMilliseconds()
    )

    private fun Student.toEntity(): StudentEntity = StudentEntity(
        id = id,
        displayName = displayName,
        email = email,
        createdAt = createdAt.toEpochMilliseconds()
    )

    private fun JoinRequest.toEntity(): JoinRequestEntity = JoinRequestEntity(
        id = id,
        studentId = studentId,
        classroomId = classroomId,
        teacherId = teacherId,
        status = status.name,
        createdAt = createdAt.toEpochMilliseconds(),
        resolvedAt = resolvedAt?.toEpochMilliseconds()
    )

    private fun ClassroomEntity.toDomain(): Classroom = Classroom(
        id = id,
        teacherId = teacherId,
        name = name,
        grade = grade,
        subject = subject,
        joinCode = joinCode,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        isArchived = isArchived,
        archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds),
        students = students
    )

    private fun TopicEntity.toDomain(): Topic = Topic(
        id = id,
        classroomId = classroomId,
        teacherId = teacherId,
        name = name,
        description = description,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        isArchived = isArchived,
        archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
    )

    private fun StudentEntity.toDomain(): Student = Student(
        id = id,
        displayName = displayName,
        email = email,
        createdAt = Instant.fromEpochMilliseconds(createdAt)
    )

     private fun JoinRequestEntity.toDomain(): JoinRequest = JoinRequest(
        id = id,
        studentId = studentId,
        classroomId = classroomId,
        teacherId = teacherId,
        status = JoinRequestStatus.valueOf(status),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        resolvedAt = resolvedAt?.let(Instant::fromEpochMilliseconds)
    )

    private fun generateLocalId(prefix: String): String = "$prefix-${Clock.System.now().toEpochMilliseconds()}"

    private suspend fun requireTeacherId(): String =
        authRepository.authState.firstOrNull()?.userId
            ?: error("No authenticated teacher available")

    private suspend fun requireStudentId(): String =
        authRepository.authState.firstOrNull()?.userId
            ?: error("No authenticated student available")

    private suspend fun currentUserId(): String? =
        authRepository.authState.firstOrNull()?.userId

    private fun shouldIgnorePermissionDenied(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    private fun isTransient(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error?.cause is java.net.UnknownHostException
}
