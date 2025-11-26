package com.classroom.quizmaster.data.repo

import androidx.room.withTransaction
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.entity.AssignmentLocalEntity
import com.classroom.quizmaster.data.local.entity.SubmissionLocalEntity
import com.classroom.quizmaster.data.remote.FirebaseAssignmentDataSource
import com.classroom.quizmaster.data.remote.FirebaseQuizDataSource
import com.classroom.quizmaster.data.sync.PendingOpQueue
import com.classroom.quizmaster.data.sync.PendingOpTypes
import com.classroom.quizmaster.data.sync.UpsertAssignmentPayload
import com.classroom.quizmaster.data.sync.ArchiveAssignmentPayload
import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber
import com.classroom.quizmaster.util.switchMapLatest
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.serialization.json.Json

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val remote: FirebaseAssignmentDataSource,
    private val quizRemote: FirebaseQuizDataSource,
    private val database: QuizMasterDatabase,
    private val authRepository: AuthRepository,
    private val pendingOpQueue: PendingOpQueue,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AssignmentRepository {

    private val assignmentDao = database.assignmentDao()
    private val classroomDao = database.classroomDao()
    private val topicDao = database.topicDao()
    private val quizDao = database.quizDao()

    override val assignments: Flow<List<Assignment>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val userId = auth.userId
                if (userId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else if (auth.isTeacher) {
                    combine(
                        assignmentDao.observeAssignments(),
                        classroomDao.observeForTeacher(userId),
                        topicDao.observeForTeacher(userId),
                        quizDao.observeActiveForTeacher(userId)
                    ) { assignments, classrooms, topics, quizzes ->
                        val activeClassrooms = classrooms.map { it.id }.toSet()
                        val activeTopics = topics.map { it.id }.toSet()
                        val quizzesById = quizzes.associateBy { it.quiz.id }
                        assignments
                            .filter { assignment ->
                                assignment.classroomId in activeClassrooms &&
                                    assignment.topicId in activeTopics &&
                                    quizzesById[assignment.quizId]?.quiz?.topicId == assignment.topicId
                            }
                            .map { it.toDomain() }
                    }
                } else {
                    combine(
                        assignmentDao.observeAssignments(),
                        classroomDao.observeForStudent(userId)
                    ) { assignments, classrooms ->
                        val allowedClassrooms = classrooms
                            .filterNot { it.isArchived }
                            .map { it.id }
                            .toSet()
                        assignments
                            .filter { assignment ->
                                assignment.classroomId in allowedClassrooms && !assignment.isArchived
                            }
                            .map { it.toDomain() }
                    }
                }
            }
            .distinctUntilChanged()

    override fun submissions(assignmentId: String): Flow<List<Submission>> =
        assignmentDao.observeSubmissions(assignmentId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun submissionsForUser(userId: String): Flow<List<Submission>> =
        assignmentDao.observeSubmissionsForUser(userId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun refreshAssignments() = withContext(ioDispatcher) {
        val remoteAssignments = remote.fetchAssignments().getOrElse { emptyList() }
        val remoteSubmissions = remoteAssignments.associate { assignment ->
            assignment.id to remote.fetchSubmissions(assignment.id).getOrElse { emptyList() }
        }
        val referencedQuizIds = remoteAssignments
            .mapNotNull { assignment -> assignment.quizId.takeIf { it.isNotBlank() } }
            .distinct()
        val referencedQuizzes = if (referencedQuizIds.isEmpty()) {
            emptyList()
        } else {
            quizRemote.loadQuizzesByIds(referencedQuizIds)
        }
        database.withTransaction {
            if (remoteAssignments.isNotEmpty()) {
                assignmentDao.upsertAssignments(remoteAssignments.map { it.toEntity() })
            }
            remoteSubmissions.forEach { (assignmentId, submissions) ->
                if (submissions.isNotEmpty()) {
                    assignmentDao.upsertSubmissions(submissions.map { it.toEntity() })
                }
            }
            referencedQuizzes.forEach { quiz ->
                val quizId = quiz.id
                quizDao.upsertQuizWithQuestions(
                    quiz.toEntity(resolvedId = quizId),
                    quiz.questions.mapIndexed { index, question ->
                        question.toEntity(quizId, index, json)
                    }
                )
            }
        }
    }

    override suspend fun createAssignment(assignment: Assignment) = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId
            ?: error("No authenticated teacher available")
        require(assignment.classroomId.isNotBlank()) { "Assignment requires a classroom" }
        require(assignment.topicId.isNotBlank()) { "Assignment requires a topic" }
        val classroom = classroomDao.get(assignment.classroomId)
            ?: error("Classroom ${assignment.classroomId} not found")
        check(classroom.teacherId == teacherId) { "Cannot assign work to another teacher's classroom" }
        check(!classroom.isArchived) { "Cannot assign work to an archived classroom" }
        val topic = topicDao.get(assignment.topicId)
            ?: error("Topic ${assignment.topicId} not found")
        check(topic.classroomId == assignment.classroomId) { "Topic ${assignment.topicId} not in classroom ${assignment.classroomId}" }
        check(topic.teacherId == teacherId) { "Cannot assign work to another teacher's topic" }
        check(!topic.isArchived) { "Cannot assign work to an archived topic" }
        val quiz = quizDao.getQuiz(assignment.quizId)
            ?: error("Quiz ${assignment.quizId} not found")
        check(!quiz.quiz.isArchived) { "Cannot assign an archived quiz" }
        check(quiz.quiz.teacherId == teacherId) { "Cannot assign another teacher's quiz" }
        check(quiz.quiz.topicId == assignment.topicId) { "Quiz ${assignment.quizId} does not belong to topic ${assignment.topicId}" }

        database.withTransaction {
            assignmentDao.upsertAssignments(listOf(assignment.toEntity()))
        }
        val remoteResult = remote.createAssignment(assignment)
        remoteResult.onFailure { err ->
            if (shouldIgnorePermissionDenied(err) || isTransient(err)) {
                Timber.w(err, "Queueing assignment create for sync: ${assignment.id}")
                pendingOpQueue.enqueue(
                    PendingOpTypes.ASSIGNMENT_UPSERT,
                    UpsertAssignmentPayload(assignment),
                    UpsertAssignmentPayload.serializer()
                )
            } else {
                Timber.e(err, "Failed to create assignment ${assignment.id}")
                throw err
            }
        }
        Unit
    }

    override suspend fun getAssignment(id: String): Assignment? = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: return@withContext null
        val stored = assignmentDao.getAssignment(id) ?: return@withContext null
        val classroom = classroomDao.get(stored.classroomId)
            ?.takeUnless { it.teacherId != teacherId }
            ?: return@withContext null
        val topic = topicDao.get(stored.topicId)
            ?.takeUnless { it.teacherId != teacherId || it.classroomId != classroom.id }
            ?: return@withContext null
        if (topic.isArchived || classroom.isArchived) return@withContext null
        stored.toDomain()
    }

    override suspend fun updateAssignment(assignment: Assignment) = withContext(ioDispatcher) {
        require(assignment.id.isNotBlank()) { "Assignment id required" }
        val teacherId = authRepository.authState.firstOrNull()?.userId
            ?: error("No authenticated teacher available")
        val existing = assignmentDao.getAssignment(assignment.id)
            ?: error("Assignment ${assignment.id} not found")
        check(!existing.isArchived) { "Cannot edit an archived assignment" }
        check(existing.classroomId == assignment.classroomId) { "Cannot move assignment to another classroom" }
        check(existing.topicId == assignment.topicId) { "Cannot move assignment to another topic" }
        val classroom = classroomDao.get(assignment.classroomId)
            ?: error("Classroom ${assignment.classroomId} not found")
        check(classroom.teacherId == teacherId) { "Cannot edit another teacher's assignment" }
        check(!classroom.isArchived) { "Cannot edit assignments from archived classrooms" }
        val topic = topicDao.get(assignment.topicId)
            ?: error("Topic ${assignment.topicId} not found")
        check(topic.classroomId == assignment.classroomId) { "Topic ${assignment.topicId} not in classroom ${assignment.classroomId}" }
        check(topic.teacherId == teacherId) { "Cannot edit another teacher's assignment" }
        check(!topic.isArchived) { "Cannot edit assignments from archived topics" }
        val quiz = quizDao.getQuiz(assignment.quizId)
            ?: error("Quiz ${assignment.quizId} not found")
        check(quiz.quiz.teacherId == teacherId) { "Cannot assign another teacher's quiz" }
        check(!quiz.quiz.isArchived) { "Cannot assign an archived quiz" }
        check(quiz.quiz.topicId == assignment.topicId) { "Quiz ${assignment.quizId} does not belong to topic ${assignment.topicId}" }

        database.withTransaction {
            assignmentDao.upsertAssignments(listOf(assignment.toEntity()))
        }
        val remoteResult = remote.updateAssignment(assignment)
        remoteResult.onFailure { err ->
            if (shouldIgnorePermissionDenied(err) || isTransient(err)) {
                Timber.w(err, "Queueing assignment update for sync: ${assignment.id}")
                pendingOpQueue.enqueue(
                    PendingOpTypes.ASSIGNMENT_UPSERT,
                    UpsertAssignmentPayload(assignment),
                    UpsertAssignmentPayload.serializer()
                )
            } else {
                Timber.e(err, "Failed to update assignment ${assignment.id}")
                throw err
            }
        }
        Unit
    }

    override suspend fun archiveAssignment(id: String, archivedAt: Instant) = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: return@withContext
        val existing = assignmentDao.getAssignment(id) ?: return@withContext
        val classroom = classroomDao.get(existing.classroomId) ?: return@withContext
        if (classroom.teacherId != teacherId) return@withContext
        val topic = topicDao.get(existing.topicId) ?: return@withContext
        if (topic.teacherId != teacherId || topic.classroomId != classroom.id) return@withContext
        val archivedEntity = existing.copy(
            isArchived = true,
            archivedAt = archivedAt.toEpochMilliseconds(),
            updatedAt = archivedAt.toEpochMilliseconds()
        )
        database.withTransaction { assignmentDao.upsertAssignments(listOf(archivedEntity)) }
        val remoteResult = remote.archiveAssignment(id, archivedAt)
        remoteResult.onFailure { err ->
            if (shouldIgnorePermissionDenied(err) || isTransient(err)) {
                Timber.w(err, "Queueing assignment archive for sync: $id")
                pendingOpQueue.enqueue(
                    PendingOpTypes.ASSIGNMENT_ARCHIVE,
                    ArchiveAssignmentPayload(id, archivedAt.toEpochMilliseconds()),
                    ArchiveAssignmentPayload.serializer()
                )
            } else {
                Timber.e(err, "Failed to archive assignment $id")
                throw err
            }
        }
        Unit
    }

    override suspend fun unarchiveAssignment(id: String, unarchivedAt: Instant) = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: return@withContext
        val existing = assignmentDao.getAssignment(id) ?: return@withContext
        val classroom = classroomDao.get(existing.classroomId) ?: return@withContext
        val topic = topicDao.get(existing.topicId) ?: return@withContext
        if (classroom.teacherId != teacherId || topic.teacherId != teacherId || topic.classroomId != classroom.id) return@withContext
        val unarchivedEntity = existing.copy(
            isArchived = false,
            archivedAt = null,
            updatedAt = unarchivedAt.toEpochMilliseconds()
        )
        database.withTransaction { assignmentDao.upsertAssignments(listOf(unarchivedEntity)) }
        val remoteResult = remote.unarchiveAssignment(id, unarchivedAt)
        remoteResult.onFailure { err ->
            if (shouldIgnorePermissionDenied(err) || isTransient(err)) {
                Timber.w(err, "Skipping remote unarchive for $id")
            } else {
                Timber.e(err, "Failed to unarchive assignment $id")
                throw err
            }
        }
        Unit
    }

    override suspend fun submitHomework(submission: Submission) = withContext(ioDispatcher) {
        database.withTransaction { assignmentDao.upsertSubmission(submission.toEntity()) }
        val remoteResult = remote.saveSubmission(submission)
        remoteResult.onFailure { err ->
            if (shouldIgnorePermissionDenied(err) || isTransient(err)) {
                Timber.w(err, "Skipping remote submission for ${submission.assignmentId}:${submission.uid}")
            } else {
                Timber.w(err, "Failed to mirror submission ${submission.assignmentId}:${submission.uid}")
                throw err
            }
        }
        Unit
    }

    private fun Assignment.toEntity(): AssignmentLocalEntity = AssignmentLocalEntity(
        id = id,
        quizId = quizId,
        classroomId = classroomId,
        topicId = topicId,
        openAt = openAt.toEpochMilliseconds(),
        closeAt = closeAt.toEpochMilliseconds(),
        attemptsAllowed = attemptsAllowed,
        scoringMode = scoringMode.name,
        revealAfterSubmit = revealAfterSubmit,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        isArchived = isArchived,
        archivedAt = archivedAt?.toEpochMilliseconds()
    )

    private fun Submission.toEntity(): SubmissionLocalEntity = SubmissionLocalEntity(
        assignmentId = assignmentId,
        uid = uid,
        bestScore = bestScore,
        lastScore = lastScore,
        attempts = attempts,
        updatedAt = updatedAt.toEpochMilliseconds()
    )

    private fun AssignmentLocalEntity.toDomain(): Assignment = Assignment(
        id = id,
        quizId = quizId,
        classroomId = classroomId,
        topicId = topicId,
        openAt = Instant.fromEpochMilliseconds(openAt),
        closeAt = Instant.fromEpochMilliseconds(closeAt),
        attemptsAllowed = attemptsAllowed,
        scoringMode = runCatching { ScoringMode.valueOf(scoringMode) }.getOrDefault(ScoringMode.BEST),
        revealAfterSubmit = revealAfterSubmit,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        isArchived = isArchived,
        archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
    )

    private fun SubmissionLocalEntity.toDomain(): Submission = Submission(
        uid = uid,
        assignmentId = assignmentId,
        bestScore = bestScore,
        lastScore = lastScore,
        attempts = attempts,
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )

    private fun shouldIgnorePermissionDenied(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    private fun isTransient(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error?.cause is java.net.UnknownHostException
}
