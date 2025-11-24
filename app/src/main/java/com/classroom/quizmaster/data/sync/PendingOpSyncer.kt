package com.classroom.quizmaster.data.sync

import com.classroom.quizmaster.data.local.dao.OpLogDao
import com.classroom.quizmaster.data.remote.FirebaseAssignmentDataSource
import com.classroom.quizmaster.data.remote.FirebaseClassroomDataSource
import com.classroom.quizmaster.data.remote.FirebaseMaterialDataSource
import com.classroom.quizmaster.data.remote.FirebaseQuizDataSource
import com.classroom.quizmaster.data.remote.FirebaseTopicDataSource
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.LearningMaterialRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class PendingOpSyncer @Inject constructor(
    private val opLogDao: OpLogDao,
    private val json: Json,
    private val classroomRemote: FirebaseClassroomDataSource,
    private val topicRemote: FirebaseTopicDataSource,
    private val quizRemote: FirebaseQuizDataSource,
    private val assignmentRemote: FirebaseAssignmentDataSource,
    private val materialRemote: FirebaseMaterialDataSource,
    private val classroomRepository: ClassroomRepository,
    private val assignmentRepository: AssignmentRepository,
    private val quizRepository: QuizRepository,
    private val learningMaterialRepository: LearningMaterialRepository
) {

    suspend fun syncPending() = withContext(Dispatchers.IO) {
        val pending = opLogDao.pendingOfTypes(SUPPORTED_TYPES, limit = 50)
        if (pending.isEmpty()) {
            refreshLocal()
            return@withContext
        }
        val synced = mutableListOf<String>()
        pending.forEach { op ->
            try {
                when (op.type) {
                    PendingOpTypes.CLASSROOM_UPSERT -> {
                        val payload = json.decodeFromString<UpsertClassroomPayload>(op.payloadJson)
                        classroomRemote.upsertClassroom(payload.classroom).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.CLASSROOM_ARCHIVE -> {
                        val payload = json.decodeFromString<ArchiveClassroomPayload>(op.payloadJson)
                        classroomRemote.archiveClassroom(
                            payload.classroomId,
                            Instant.fromEpochMilliseconds(payload.archivedAt)
                        ).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.TOPIC_UPSERT -> {
                        val payload = json.decodeFromString<UpsertTopicPayload>(op.payloadJson)
                        topicRemote.upsertTopic(payload.topic).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.TOPIC_ARCHIVE -> {
                        val payload = json.decodeFromString<ArchiveTopicPayload>(op.payloadJson)
                        topicRemote.archiveTopic(
                            payload.topicId,
                            Instant.fromEpochMilliseconds(payload.archivedAt)
                        ).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.QUIZ_UPSERT -> {
                        val payload = json.decodeFromString<UpsertQuizPayload>(op.payloadJson)
                        quizRemote.upsertQuiz(payload.quiz).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.QUIZ_ARCHIVE -> {
                        val payload = json.decodeFromString<ArchiveQuizPayload>(op.payloadJson)
                        quizRemote.archiveQuiz(
                            payload.quizId,
                            Instant.fromEpochMilliseconds(payload.archivedAt)
                        ).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.ASSIGNMENT_UPSERT -> {
                        val payload = json.decodeFromString<UpsertAssignmentPayload>(op.payloadJson)
                        assignmentRemote.updateAssignment(payload.assignment).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.ASSIGNMENT_ARCHIVE -> {
                        val payload = json.decodeFromString<ArchiveAssignmentPayload>(op.payloadJson)
                        assignmentRemote.archiveAssignment(
                            payload.assignmentId,
                            Instant.fromEpochMilliseconds(payload.archivedAt)
                        ).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.MATERIAL_UPSERT -> {
                        val payload = json.decodeFromString<UpsertMaterialPayload>(op.payloadJson)
                        materialRemote.upsertMaterial(payload.material).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.MATERIAL_ARCHIVE -> {
                        val payload = json.decodeFromString<ArchiveMaterialPayload>(op.payloadJson)
                        materialRemote.archiveMaterial(
                            payload.materialId,
                            Instant.fromEpochMilliseconds(payload.archivedAt)
                        ).getOrThrow()
                        synced += op.id
                    }
                    PendingOpTypes.MATERIAL_DELETE -> {
                        val payload = json.decodeFromString<DeleteMaterialPayload>(op.payloadJson)
                        materialRemote.deleteMaterial(payload.materialId).getOrThrow()
                        synced += op.id
                    }
                }
            } catch (error: Exception) {
                opLogDao.incrementRetry(op.id)
                Timber.w(error, "Pending op sync failed for ${op.type}:${op.id}")
            }
        }
        if (synced.isNotEmpty()) {
            opLogDao.markSynced(synced)
            opLogDao.deleteSynced()
        }
        refreshLocal()
    }

    private suspend fun refreshLocal() {
        runCatching { classroomRepository.refresh() }.onFailure { Timber.w(it, "Classroom refresh after sync failed") }
        runCatching { assignmentRepository.refreshAssignments() }.onFailure { Timber.w(it, "Assignment refresh after sync failed") }
        runCatching { quizRepository.refresh() }.onFailure { Timber.w(it, "Quiz refresh after sync failed") }
        runCatching { learningMaterialRepository.refreshMetadata() }.onFailure { Timber.w(it, "Material metadata refresh after sync failed") }
    }

    private companion object {
        val SUPPORTED_TYPES: List<String> = listOf(
            PendingOpTypes.CLASSROOM_UPSERT,
            PendingOpTypes.CLASSROOM_ARCHIVE,
            PendingOpTypes.TOPIC_UPSERT,
            PendingOpTypes.TOPIC_ARCHIVE,
            PendingOpTypes.QUIZ_UPSERT,
            PendingOpTypes.QUIZ_ARCHIVE,
            PendingOpTypes.ASSIGNMENT_UPSERT,
            PendingOpTypes.ASSIGNMENT_ARCHIVE,
            PendingOpTypes.MATERIAL_UPSERT,
            PendingOpTypes.MATERIAL_ARCHIVE,
            PendingOpTypes.MATERIAL_DELETE
        )
    }
}
