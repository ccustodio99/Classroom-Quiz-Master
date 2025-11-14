package com.classroom.quizmaster.data.repo

import androidx.room.withTransaction
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.dao.ClassroomDao
import com.classroom.quizmaster.data.local.dao.TopicDao
import com.classroom.quizmaster.data.local.entity.ClassroomEntity
import com.classroom.quizmaster.data.local.entity.TopicEntity
import com.classroom.quizmaster.data.remote.FirebaseClassroomDataSource
import com.classroom.quizmaster.data.remote.FirebaseTopicDataSource
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Topic
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
    private val classroomRemote: FirebaseClassroomDataSource,
    private val topicRemote: FirebaseTopicDataSource,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ClassroomRepository {

    override val classrooms: Flow<List<Classroom>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val teacherId = auth.userId
                if (teacherId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    classroomDao.observeForTeacher(teacherId)
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
                val teacherId = auth.userId
                if (teacherId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        classroomDao.observeForTeacher(teacherId),
                        topicDao.observeForTeacher(teacherId)
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

    override suspend fun refresh() = withContext(ioDispatcher) {
        val classrooms = classroomRemote.fetchClassrooms().getOrElse { emptyList() }
        val topics = topicRemote.fetchTopics().getOrElse { emptyList() }
        if (classrooms.isEmpty() && topics.isEmpty()) return@withContext
        database.withTransaction {
            if (classrooms.isNotEmpty()) {
                classroomDao.upsertAll(classrooms.map { it.toEntity() })
            }
            if (topics.isNotEmpty()) {
                topicDao.upsertAll(topics.map { it.toEntity() })
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

    override suspend fun getClassroom(id: String): Classroom? = withContext(ioDispatcher) {
        val teacherId = currentTeacherId() ?: return@withContext null
        classroomDao.get(id)
            ?.takeUnless { it.isArchived || it.teacherId != teacherId }
            ?.toDomain()
    }

    override suspend fun getTopic(id: String): Topic? = withContext(ioDispatcher) {
        val teacherId = currentTeacherId() ?: return@withContext null
        val entity = topicDao.get(id)
            ?.takeUnless { it.isArchived || it.teacherId != teacherId }
            ?: return@withContext null
        classroomDao.get(entity.classroomId)
            ?.takeUnless { it.isArchived || it.teacherId != teacherId }
            ?: return@withContext null
        entity.toDomain()
    }

    private fun Classroom.toEntity(): ClassroomEntity = ClassroomEntity(
        id = id.ifBlank { generateLocalId("classroom") },
        teacherId = teacherId,
        name = name,
        grade = grade,
        subject = subject,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        isArchived = isArchived,
        archivedAt = archivedAt?.toEpochMilliseconds()
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

    private fun ClassroomEntity.toDomain(): Classroom = Classroom(
        id = id,
        teacherId = teacherId,
        name = name,
        grade = grade,
        subject = subject,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        isArchived = isArchived,
        archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
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

    private fun generateLocalId(prefix: String): String = "$prefix-${Clock.System.now().toEpochMilliseconds()}"

    private suspend fun requireTeacherId(): String =
        authRepository.authState.firstOrNull()?.userId
            ?: error("No authenticated teacher available")

    private suspend fun currentTeacherId(): String? =
        authRepository.authState.firstOrNull()?.userId

    private fun shouldIgnorePermissionDenied(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    private fun isTransient(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error?.cause is java.net.UnknownHostException
}
