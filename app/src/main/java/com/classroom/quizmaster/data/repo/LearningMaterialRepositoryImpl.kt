package com.classroom.quizmaster.data.repo

import androidx.room.withTransaction
import com.classroom.quizmaster.data.lan.LanHostServer
import com.classroom.quizmaster.data.lan.WireMessage
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.dao.ClassroomDao
import com.classroom.quizmaster.data.local.dao.MaterialDao
import com.classroom.quizmaster.data.local.dao.SessionDao
import com.classroom.quizmaster.data.local.dao.TopicDao
import com.classroom.quizmaster.data.local.entity.ClassroomEntity
import com.classroom.quizmaster.data.local.entity.LearningMaterialEntity
import com.classroom.quizmaster.data.local.entity.MaterialAttachmentEntity
import com.classroom.quizmaster.data.local.entity.MaterialWithAttachments
import com.classroom.quizmaster.data.local.entity.TopicEntity
import com.classroom.quizmaster.data.remote.FirebaseMaterialDataSource
import com.classroom.quizmaster.data.sync.PendingOpQueue
import com.classroom.quizmaster.data.sync.PendingOpTypes
import com.classroom.quizmaster.data.sync.UpsertMaterialPayload
import com.classroom.quizmaster.data.sync.ArchiveMaterialPayload
import com.classroom.quizmaster.data.sync.DeleteMaterialPayload
import com.classroom.quizmaster.domain.model.LearningMaterial
import com.classroom.quizmaster.domain.model.MaterialAttachment
import com.classroom.quizmaster.domain.model.MaterialAttachmentType
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.LearningMaterialRepository
import com.classroom.quizmaster.util.switchMapLatest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import timber.log.Timber

@Singleton
class LearningMaterialRepositoryImpl @Inject constructor(
    private val database: QuizMasterDatabase,
    private val materialDao: MaterialDao,
    private val classroomDao: ClassroomDao,
    private val topicDao: TopicDao,
    private val sessionDao: SessionDao,
    private val authRepository: AuthRepository,
    private val lanHostServer: LanHostServer,
    private val materialRemote: FirebaseMaterialDataSource,
    private val pendingOpQueue: PendingOpQueue,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LearningMaterialRepository {

    private val opLogDao = database.opLogDao()

    override fun observeTeacherMaterials(
        classroomId: String?,
        topicId: String?,
        includeArchived: Boolean
    ): Flow<List<LearningMaterial>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val teacherId = auth.userId
                if (teacherId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    val baseFlow = if (includeArchived) {
                        materialDao.observeArchivedForTeacher(teacherId)
                    } else {
                        materialDao.observeActiveForTeacher(teacherId)
                    }
                    baseFlow.map { list ->
                        list
                            .filter { classroomId == null || it.material.classroomId == classroomId }
                            .filter { topicId == null || it.material.topicId == topicId }
                            .map { it.toDomain(json) }
                    }
                }
            }
            .distinctUntilChanged()

    override fun observeStudentMaterials(
        classroomId: String?,
        topicId: String?
    ): Flow<List<LearningMaterial>> =
        materialDao.observeAllActive()
            .map { list ->
                list
                    .filter { classroomId == null || it.material.classroomId == classroomId }
                    .filter { topicId == null || it.material.topicId == topicId }
                    .map { it.toDomain(json) }
            }
            .distinctUntilChanged()

    override fun observeMaterial(materialId: String): Flow<LearningMaterial?> =
        materialDao.observeMaterial(materialId)
            .map { it?.toDomain(json) }
            .distinctUntilChanged()

    override suspend fun get(materialId: String): LearningMaterial? = withContext(ioDispatcher) {
        materialDao.getMaterial(materialId)?.toDomain(json)
    }

    private fun LearningMaterialEntity.toDomainModelWithoutAttachments(): LearningMaterial =
        LearningMaterial(
            id = id,
            teacherId = teacherId,
            classroomId = classroomId,
            classroomName = classroomName,
            topicId = topicId,
            topicName = topicName,
            title = title,
            description = description,
            body = body,
            attachments = emptyList(),
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            isArchived = isArchived,
            archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
        )

    override suspend fun refreshMetadata() = withContext(ioDispatcher) {
        if (hasPendingOps()) return@withContext
        val authState = authRepository.authState.firstOrNull() ?: return@withContext
        val materials = when {
            authState.role == com.classroom.quizmaster.domain.model.UserRole.TEACHER -> {
                val teacherId = authState.userId ?: return@withContext
                materialRemote.fetchForTeacher(teacherId).getOrElse {
                    Timber.w(it, "Failed to fetch materials for teacher")
                    emptyList()
                }
            }
            else -> {
                val studentId = authState.userId ?: return@withContext
                val classrooms = classroomDao.listForStudent(studentId)
                val classroomIds = classrooms.map { it.id }
                materialRemote.fetchForClassrooms(classroomIds).getOrElse {
                    Timber.w(it, "Failed to fetch materials for student classrooms")
                    emptyList()
                }
            }
        }
        if (materials.isEmpty()) return@withContext
        database.withTransaction {
            materials.forEach { material ->
                materialDao.upsertMaterial(material.toEntity())
            }
        }
    }

    override suspend fun upsert(material: LearningMaterial): String = withContext(ioDispatcher) {
        val authState = authRepository.authState.firstOrNull()
            ?: error("No authenticated teacher available")
        val teacherId = authState.userId ?: error("No authenticated teacher available")
        val classroomId = material.classroomId.takeIf { it.isNotBlank() }
            ?: error("Classroom is required")
        val classroom = classroomDao.get(classroomId)
            ?: error("Classroom $classroomId not found")
        check(classroom.teacherId == teacherId) { "Cannot use another teacher's classroom" }
        val topicId = material.topicId.takeIf { it.isNotBlank() }
        val topic = topicId?.let { topicDao.get(it) }
        if (topicId != null) {
            requireNotNull(topic) { "Topic $topicId not found" }
            check(topic.classroomId == classroomId) { "Topic does not belong to classroom" }
            check(topic.teacherId == teacherId) { "Cannot use another teacher's topic" }
        }
        val now = Clock.System.now()
        val resolvedMaterialId = material.id.ifBlank { generateMaterialId() }
        val createdAt = material.createdAt.takeIf { material.id.isNotBlank() } ?: now
        val normalized = material.copy(
            id = resolvedMaterialId,
            teacherId = teacherId,
            classroomId = classroomId,
            classroomName = classroom.name,
            topicId = topic?.id.orEmpty(),
            topicName = topic?.name.orEmpty(),
            createdAt = createdAt,
            updatedAt = now,
            attachments = material.attachments.map { attachment ->
                val resolvedAttachmentId = attachment.id.ifBlank { generateAttachmentId() }
                attachment.copy(
                    id = resolvedAttachmentId,
                    materialId = resolvedMaterialId
                )
            }
        )
        database.withTransaction {
            materialDao.upsertMaterial(normalized.toEntity())
            if (normalized.attachments.isNotEmpty()) {
                materialDao.upsertAttachments(normalized.attachments.map { it.toEntity(json) })
                val keepIds = normalized.attachments.map { it.id }
                materialDao.pruneAttachments(normalized.id, keepIds)
            } else {
                materialDao.clearAttachments(normalized.id)
            }
        }
        val remoteMaterial = normalized.copy(attachments = emptyList())
        materialRemote.upsertMaterial(remoteMaterial)
            .onFailure {
                Timber.w(it, "Queueing material upsert for sync: ${normalized.id}")
                pendingOpQueue.enqueue(
                    PendingOpTypes.MATERIAL_UPSERT,
                    UpsertMaterialPayload(remoteMaterial),
                    UpsertMaterialPayload.serializer()
                )
            }
        broadcastIfHosting(normalized.classroomId)
        resolvedMaterialId
    }

    override suspend fun archive(materialId: String, archivedAt: Instant): Unit = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: return@withContext
        val stored = materialDao.getMaterial(materialId) ?: return@withContext
        if (stored.material.teacherId != teacherId) return@withContext
        val archivedEntity = stored.material.copy(
            isArchived = true,
            archivedAt = archivedAt.toEpochMilliseconds(),
            updatedAt = archivedAt.toEpochMilliseconds()
        )
        database.withTransaction {
            materialDao.upsertMaterial(archivedEntity)
        }
        materialRemote.archiveMaterial(materialId, archivedAt)
            .onFailure {
                Timber.w(it, "Queueing material archive for sync: $materialId")
                pendingOpQueue.enqueue(
                    PendingOpTypes.MATERIAL_ARCHIVE,
                    ArchiveMaterialPayload(materialId, archivedAt.toEpochMilliseconds()),
                    ArchiveMaterialPayload.serializer()
                )
            }
        broadcastIfHosting(archivedEntity.classroomId)
    }

    override suspend fun delete(materialId: String): Unit = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: return@withContext
        val stored = materialDao.getMaterial(materialId) ?: return@withContext
        if (stored.material.teacherId != teacherId) return@withContext
        database.withTransaction {
            materialDao.clearAttachments(materialId)
            materialDao.deleteMaterial(materialId)
        }
        materialRemote.deleteMaterial(materialId)
            .onFailure {
                Timber.w(it, "Queueing material delete for sync: $materialId")
                pendingOpQueue.enqueue(
                    PendingOpTypes.MATERIAL_DELETE,
                    DeleteMaterialPayload(materialId),
                    DeleteMaterialPayload.serializer()
                )
            }
        broadcastIfHosting(stored.material.classroomId)
    }

    override suspend fun move(
        materialId: String,
        targetClassroomId: String,
        targetTopicId: String?
    ): Unit = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: error("No authenticated teacher")
        val stored = materialDao.getMaterial(materialId) ?: error("Material $materialId not found")
        check(stored.material.teacherId == teacherId) { "Cannot move another teacher's material" }
        val (classroom, topic) = resolveDestination(teacherId, targetClassroomId, targetTopicId)
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = stored.material.copy(
            classroomId = classroom.id,
            classroomName = classroom.name,
            topicId = topic?.id.orEmpty(),
            topicName = topic?.name.orEmpty(),
            updatedAt = now
        )
        database.withTransaction {
            materialDao.upsertMaterial(updated)
        }
        materialRemote.upsertMaterial(updated.toDomainModelWithoutAttachments())
            .onFailure {
                Timber.w(it, "Queueing material move sync: ${updated.id}")
                pendingOpQueue.enqueue(
                    PendingOpTypes.MATERIAL_UPSERT,
                    UpsertMaterialPayload(updated.toDomainModelWithoutAttachments()),
                    UpsertMaterialPayload.serializer()
                )
            }
        broadcastIfHosting(stored.material.classroomId)
        broadcastIfHosting(classroom.id)
    }

    override suspend fun duplicate(
        materialId: String,
        targetClassroomId: String,
        targetTopicId: String?
    ): String = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: error("No authenticated teacher")
        val stored = materialDao.getMaterial(materialId) ?: error("Material $materialId not found")
        check(stored.material.teacherId == teacherId) { "Cannot copy another teacher's material" }
        val (classroom, topic) = resolveDestination(teacherId, targetClassroomId, targetTopicId)
        val now = Clock.System.now().toEpochMilliseconds()
        val newMaterialId = UUID.randomUUID().toString()
        val duplicated = stored.material.copy(
            id = newMaterialId,
            classroomId = classroom.id,
            classroomName = classroom.name,
            topicId = topic?.id.orEmpty(),
            topicName = topic?.name.orEmpty(),
            createdAt = now,
            updatedAt = now,
            isArchived = false,
            archivedAt = null
        )
        val attachments = stored.attachments.map { attachment ->
            attachment.copy(
                id = UUID.randomUUID().toString(),
                materialId = newMaterialId,
                downloadedAt = null
            )
        }
        database.withTransaction {
            materialDao.upsertMaterial(duplicated)
            if (attachments.isNotEmpty()) {
                materialDao.upsertAttachments(attachments)
            }
        }
        materialRemote.upsertMaterial(duplicated.toDomainModelWithoutAttachments())
            .onFailure {
                Timber.w(it, "Queueing material duplicate sync: ${duplicated.id}")
                pendingOpQueue.enqueue(
                    PendingOpTypes.MATERIAL_UPSERT,
                    UpsertMaterialPayload(duplicated.toDomainModelWithoutAttachments()),
                    UpsertMaterialPayload.serializer()
                )
            }
        broadcastIfHosting(classroom.id)
        newMaterialId
    }

    override suspend fun shareSnapshotForClassroom(classroomId: String): Unit = withContext(ioDispatcher) {
        if (lanHostServer.activePort == null) return@withContext
        val snapshot = materialDao.listActiveForClassroom(classroomId)
        if (snapshot.isEmpty()) return@withContext
        val payload = json.encodeToString(snapshot.map { it.toDomain(json) })
        lanHostServer.broadcast(WireMessage.MaterialsSnapshot(classroomId, payload))
    }

    override suspend fun importSnapshot(
        classroomId: String,
        materials: List<LearningMaterial>
    ): Unit = withContext(ioDispatcher) {
        database.withTransaction {
            materialDao.deleteAttachmentsForClassroom(classroomId)
            materialDao.deleteMaterialsForClassroom(classroomId)
            materials.forEach { material ->
                materialDao.upsertMaterial(material.toEntity())
                if (material.attachments.isNotEmpty()) {
                    materialDao.upsertAttachments(material.attachments.map { it.toEntity(json) })
                }
            }
        }
    }

    private suspend fun broadcastIfHosting(classroomId: String) {
        if (lanHostServer.activePort == null) return
        val currentSession = sessionDao.currentSession() ?: return
        if (currentSession.classroomId != classroomId) return
        shareSnapshotForClassroom(classroomId)
    }

    private suspend fun resolveDestination(
        teacherId: String,
        classroomId: String,
        topicId: String?
    ): Pair<ClassroomEntity, TopicEntity?> {
        val classroom = classroomDao.get(classroomId) ?: error("Classroom $classroomId not found")
        check(classroom.teacherId == teacherId) { "Cannot target another teacher's classroom" }
        val normalizedTopicId = topicId?.takeIf { it.isNotBlank() }
        val topic = normalizedTopicId?.let { candidateId ->
            val entity = topicDao.get(candidateId) ?: error("Topic $candidateId not found")
            check(entity.teacherId == teacherId) { "Cannot target another teacher's topic" }
            check(entity.classroomId == classroom.id) { "Topic does not belong to classroom ${classroom.name}" }
            entity
        }
        return classroom to topic
    }

    private fun LearningMaterial.toEntity(): LearningMaterialEntity =
        LearningMaterialEntity(
            id = id,
            teacherId = teacherId,
            classroomId = classroomId,
            classroomName = classroomName,
            topicId = topicId,
            topicName = topicName,
            title = title,
            description = description,
            body = body,
            createdAt = createdAt.toEpochMilliseconds(),
            updatedAt = updatedAt.toEpochMilliseconds(),
            isArchived = isArchived,
            archivedAt = archivedAt?.toEpochMilliseconds()
        )

    private fun MaterialAttachment.toEntity(json: Json): MaterialAttachmentEntity =
        MaterialAttachmentEntity(
            id = id,
            materialId = materialId,
            displayName = displayName,
            type = type.name,
            uri = uri,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            downloadedAt = downloadedAt?.toEpochMilliseconds(),
            metadataJson = json.encodeToString(metadata)
        )

    private fun MaterialWithAttachments.toDomain(json: Json): LearningMaterial =
        LearningMaterial(
            id = material.id,
            teacherId = material.teacherId,
            classroomId = material.classroomId,
            classroomName = material.classroomName,
            topicId = material.topicId,
            topicName = material.topicName,
            title = material.title,
            description = material.description,
            body = material.body,
            createdAt = Instant.fromEpochMilliseconds(material.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(material.updatedAt),
            isArchived = material.isArchived,
            archivedAt = material.archivedAt?.let(Instant::fromEpochMilliseconds),
            attachments = attachments.map { entity ->
                val metadata: Map<String, String> = runCatching {
                    json.decodeFromString<Map<String, String>>(entity.metadataJson)
                }.getOrDefault(emptyMap())
                MaterialAttachment(
                    id = entity.id,
                    materialId = entity.materialId,
                    displayName = entity.displayName,
                    type = runCatching { MaterialAttachmentType.valueOf(entity.type) }
                        .getOrDefault(MaterialAttachmentType.TEXT),
                    uri = entity.uri,
                    mimeType = entity.mimeType,
                    sizeBytes = entity.sizeBytes,
                    downloadedAt = entity.downloadedAt?.let(Instant::fromEpochMilliseconds),
                    metadata = metadata
                )
            }
        )

    private fun generateMaterialId(): String = "mat-${UUID.randomUUID()}"

    private fun generateAttachmentId(): String = "att-${UUID.randomUUID()}"

    private suspend fun hasPendingOps(): Boolean =
        opLogDao.pending(limit = 1).isNotEmpty()
}
