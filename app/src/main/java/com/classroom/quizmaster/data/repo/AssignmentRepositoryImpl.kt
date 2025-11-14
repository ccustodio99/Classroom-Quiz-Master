package com.classroom.quizmaster.data.repo

import androidx.room.withTransaction
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.entity.AssignmentLocalEntity
import com.classroom.quizmaster.data.local.entity.SubmissionLocalEntity
import com.classroom.quizmaster.data.remote.FirebaseAssignmentDataSource
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
import kotlinx.datetime.Instant
import timber.log.Timber
import com.classroom.quizmaster.util.switchMapLatest
import com.google.firebase.firestore.FirebaseFirestoreException

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val remote: FirebaseAssignmentDataSource,
    private val database: QuizMasterDatabase,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AssignmentRepository {

    private val assignmentDao = database.assignmentDao()
    private val classroomDao = database.classroomDao()
    private val topicDao = database.topicDao()
    private val quizDao = database.quizDao()

    override val assignments: Flow<List<Assignment>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val teacherId = auth.userId
                if (teacherId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        assignmentDao.observeAssignments(),
                        classroomDao.observeForTeacher(teacherId),
                        topicDao.observeForTeacher(teacherId),
                        quizDao.observeActiveForTeacher(teacherId)
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
                }
            }
            .distinctUntilChanged()

    override fun submissions(assignmentId: String): Flow<List<Submission>> =
        assignmentDao.observeSubmissions(assignmentId)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun refreshAssignments() = withContext(ioDispatcher) {
        val remoteAssignments = remote.fetchAssignments().getOrElse { emptyList() }
        val remoteSubmissions = remoteAssignments.associate { assignment ->
            assignment.id to remote.fetchSubmissions(assignment.id).getOrElse { emptyList() }
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
                Timber.w(err, "Skipping remote assignment create ${assignment.id}")
            } else {
                Timber.e(err, "Failed to create assignment ${assignment.id}")
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
