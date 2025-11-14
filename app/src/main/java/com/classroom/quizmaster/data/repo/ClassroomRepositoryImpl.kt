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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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
        authRepository.authState.flatMapLatest { auth ->
            val teacherId = auth.userId ?: return@flatMapLatest flowOf(emptyList())
            classroomDao.observeForTeacher(teacherId)
                .map { entities -> entities.map { it.toDomain() } }
        }
            .distinctUntilChanged()

    override val topics: Flow<List<Topic>> =
        authRepository.authState.flatMapLatest { auth ->
            val teacherId = auth.userId ?: return@flatMapLatest flowOf(emptyList())
            topicDao.observeForTeacher(teacherId)
                .map { entities -> entities.map { it.toDomain() } }
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
        val now = Clock.System.now()
        val resolvedId = classroom.id.ifBlank { generateLocalId("classroom") }
        val createdAt = if (classroom.createdAt > Instant.DISTANT_PAST) classroom.createdAt else now
        val normalized = classroom.copy(
            id = resolvedId,
            createdAt = createdAt,
            updatedAt = now,
            archivedAt = if (classroom.isArchived) classroom.archivedAt ?: now else null
        )
        database.withTransaction { classroomDao.upsert(normalized.toEntity()) }
        val remoteId = classroomRemote.upsertClassroom(normalized)
            .onFailure { throw it }
            .getOrThrow()
        if (remoteId != resolvedId) {
            val reconciled = normalized.copy(id = remoteId)
            database.withTransaction { classroomDao.upsert(reconciled.toEntity()) }
            remoteId
        } else {
            resolvedId
        }
    }

    override suspend fun archiveClassroom(classroomId: String, archivedAt: Instant) = withContext(ioDispatcher) {
        val existing = classroomDao.get(classroomId) ?: return@withContext
        val archived = existing.copy(
            updatedAt = archivedAt.toEpochMilliseconds(),
            isArchived = true,
            archivedAt = archivedAt.toEpochMilliseconds()
        )
        database.withTransaction { classroomDao.upsert(archived) }
        classroomRemote.archiveClassroom(classroomId, archivedAt)
            .onFailure { throw it }
            .getOrThrow()
    }

    override suspend fun upsertTopic(topic: Topic): String = withContext(ioDispatcher) {
        val now = Clock.System.now()
        val resolvedId = topic.id.ifBlank { generateLocalId("topic") }
        val createdAt = if (topic.createdAt > Instant.DISTANT_PAST) topic.createdAt else now
        val parent = classroomDao.get(topic.classroomId)
            ?: error("Parent classroom ${topic.classroomId} not found")
        check(!parent.isArchived) { "Cannot attach a topic to an archived classroom" }
        val normalized = topic.copy(
            id = resolvedId,
            classroomId = topic.classroomId,
            updatedAt = now,
            archivedAt = if (topic.isArchived) topic.archivedAt ?: now else null,
            createdAt = createdAt
        )
        database.withTransaction { topicDao.upsert(normalized.toEntity()) }
        val remoteId = topicRemote.upsertTopic(normalized)
            .onFailure { throw it }
            .getOrThrow()
        if (remoteId != resolvedId) {
            val reconciled = normalized.copy(id = remoteId)
            database.withTransaction { topicDao.upsert(reconciled.toEntity()) }
            remoteId
        } else {
            resolvedId
        }
    }

    override suspend fun archiveTopic(topicId: String, archivedAt: Instant) = withContext(ioDispatcher) {
        val existing = topicDao.get(topicId) ?: return@withContext
        val archived = existing.copy(
            updatedAt = archivedAt.toEpochMilliseconds(),
            isArchived = true,
            archivedAt = archivedAt.toEpochMilliseconds()
        )
        database.withTransaction { topicDao.upsert(archived) }
        topicRemote.archiveTopic(topicId, archivedAt)
            .onFailure { throw it }
            .getOrThrow()
    }

    override suspend fun getClassroom(id: String): Classroom? = withContext(ioDispatcher) {
        classroomDao.get(id)?.toDomain()
    }

    override suspend fun getTopic(id: String): Topic? = withContext(ioDispatcher) {
        topicDao.get(id)?.toDomain()
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
}
