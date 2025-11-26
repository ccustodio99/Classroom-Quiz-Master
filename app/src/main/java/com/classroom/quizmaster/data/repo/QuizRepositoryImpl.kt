package com.classroom.quizmaster.data.repo

import androidx.room.withTransaction
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.dao.QuizWithQuestions
import com.classroom.quizmaster.data.local.entity.ClassroomEntity
import com.classroom.quizmaster.data.local.entity.QuestionEntity
import com.classroom.quizmaster.data.local.entity.QuizEntity
import com.classroom.quizmaster.data.local.entity.TopicEntity
import com.classroom.quizmaster.data.remote.FirebaseClassroomDataSource
import com.classroom.quizmaster.data.remote.FirebaseQuizDataSource
import com.classroom.quizmaster.data.remote.FirebaseTopicDataSource
import com.classroom.quizmaster.data.sync.PendingOpQueue
import com.classroom.quizmaster.data.sync.PendingOpTypes
import com.classroom.quizmaster.data.sync.UpsertQuizPayload
import com.classroom.quizmaster.data.sync.ArchiveQuizPayload
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.MediaAsset
import com.classroom.quizmaster.domain.model.MediaType
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.util.JoinCodeGenerator
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
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import com.classroom.quizmaster.util.switchMapLatest
import com.google.firebase.firestore.FirebaseFirestoreException

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val remote: FirebaseQuizDataSource,
    private val classroomRemote: FirebaseClassroomDataSource,
    private val topicRemote: FirebaseTopicDataSource,
    private val database: QuizMasterDatabase,
    private val json: Json,
    private val authRepository: AuthRepository,
    private val pendingOpQueue: PendingOpQueue,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : QuizRepository {

    private val quizDao = database.quizDao()
    private val classroomDao = database.classroomDao()
    private val topicDao = database.topicDao()
    private val opLogDao = database.opLogDao()

    override val quizzes: Flow<List<Quiz>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val userId = auth.userId
                if (userId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else if (auth.isTeacher) {
                    combine(
                        quizDao.observeActiveForTeacher(userId),
                        classroomDao.observeForTeacher(userId),
                        topicDao.observeForTeacher(userId)
                    ) { stored, classrooms, topics ->
                        val classroomIds = classrooms.map { it.id }.toSet()
                        val topicIds = topics.map { it.id }.toSet()
                        stored
                            .filter { quiz ->
                                quiz.quiz.classroomId in classroomIds &&
                                    quiz.quiz.topicId in topicIds
                            }
                            .map { it.toDomain(json) }
                    }
                } else {
                    classroomDao.observeForStudent(userId)
                        .switchMapLatest { classrooms ->
                            val activeClassroomIds = classrooms
                                .filterNot { it.isArchived }
                                .map { it.id }
                            if (activeClassroomIds.isEmpty()) {
                                flowOf(emptyList())
                            } else {
                                quizDao.observeActiveForClassrooms(activeClassroomIds)
                            }
                        }
                        .map { stored -> stored.map { it.toDomain(json) } }
                }
            }
            .distinctUntilChanged()

    override suspend fun refresh() = withContext(ioDispatcher) {
        // Students should not pull teacher-owned quiz lists; rules will reject those reads.
        val auth = authRepository.authState.firstOrNull()
        if (auth?.isTeacher != true) return@withContext
        if (hasPendingOps()) return@withContext
        val remoteQuizzes = remote.loadQuizzes()
        if (remoteQuizzes.isEmpty()) return@withContext
        database.withTransaction {
            remoteQuizzes
                .filter { it.classroomId.isNotBlank() && it.topicId.isNotBlank() }
                .forEach { quiz ->
                    val quizId = quiz.id.ifBlank { generateLocalQuizId() }
                    val resolved = if (quiz.id == quizId) quiz else quiz.copy(id = quizId)
                    quizDao.upsertQuizWithQuestions(
                        resolved.toEntity(),
                        resolved.questions.mapIndexed { index, question ->
                            question.toEntity(quizId, index, json)
                        }
                    )
                }
        }
    }

    override suspend fun upsert(quiz: Quiz) = withContext(ioDispatcher) {
        val resolvedId = quiz.id.ifBlank { generateLocalQuizId() }
        val now = Clock.System.now()
        val authState = authRepository.authState.firstOrNull()
            ?: error("No authenticated teacher available")
        val teacherId = authState.userId ?: error("No authenticated teacher available")
        require(quiz.classroomId.isNotBlank()) { "Quiz must have a classroom" }
        require(quiz.topicId.isNotBlank()) { "Quiz must have a topic" }
        val classroom = classroomDao.get(quiz.classroomId)
            ?: error("Parent classroom ${quiz.classroomId} not found")
        check(classroom.teacherId == teacherId) { "Cannot use another teacher's classroom" }
        check(!classroom.isArchived) { "Cannot attach quiz to archived classroom" }
        val topic = topicDao.get(quiz.topicId)
            ?: error("Parent topic ${quiz.topicId} not found")
        check(topic.classroomId == quiz.classroomId) { "Topic ${quiz.topicId} not in classroom ${quiz.classroomId}" }
        check(topic.teacherId == teacherId) { "Cannot use another teacher's topic" }
        check(!topic.isArchived) { "Cannot attach quiz to archived topic" }

        val normalized = if (quiz.id == resolvedId) {
            quiz.copy(updatedAt = now, teacherId = teacherId)
        } else {
            quiz.copy(id = resolvedId, updatedAt = now, teacherId = teacherId)
        }
        database.withTransaction {
            quizDao.upsertQuizWithQuestions(
                normalized.toEntity(now, resolvedId),
                normalized.questions.mapIndexed { index, question ->
                    question.toEntity(resolvedId, index, json)
                }
            )
        }
        val remoteResult = remote.upsertQuiz(normalized)
        remoteResult.onFailure { err ->
            if (shouldIgnorePermissionDenied(err) || isTransient(err)) {
                Timber.w(err, "Queueing quiz upsert for sync: ${normalized.id}")
                pendingOpQueue.enqueue(
                    PendingOpTypes.QUIZ_UPSERT,
                    UpsertQuizPayload(normalized),
                    UpsertQuizPayload.serializer()
                )
            } else {
                Timber.e(err, "Failed to upsert quiz ${normalized.id}")
                throw err
            }
        }
        Unit
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: return@withContext
        val existing = quizDao.getQuiz(id) ?: return@withContext
        if (existing.quiz.teacherId != teacherId) return@withContext
        val now = Clock.System.now()
        val archivedEntity = existing.quiz.copy(
            isArchived = true,
            archivedAt = now.toEpochMilliseconds(),
            updatedAt = now.toEpochMilliseconds()
        )
        database.withTransaction {
            quizDao.upsertQuizWithQuestions(
                archivedEntity,
                existing.questions
            )
        }
        val remoteResult = remote.archiveQuiz(id, now)
        remoteResult.onFailure { err ->
            if (shouldIgnorePermissionDenied(err) || isTransient(err)) {
                Timber.w(err, "Queueing quiz archive for sync: $id")
                pendingOpQueue.enqueue(
                    PendingOpTypes.QUIZ_ARCHIVE,
                    ArchiveQuizPayload(id, now.toEpochMilliseconds()),
                    ArchiveQuizPayload.serializer()
                )
            } else {
                Timber.e(err, "Failed to archive quiz $id remotely")
                throw err
            }
        }
        Unit
    }

    override suspend fun getQuiz(id: String): Quiz? = withContext(ioDispatcher) {
        val teacherId = authRepository.authState.firstOrNull()?.userId ?: return@withContext null
        val stored = quizDao.getQuiz(id) ?: return@withContext null
        if (stored.quiz.teacherId != teacherId || stored.quiz.isArchived) return@withContext null
        val classroom = classroomDao.get(stored.quiz.classroomId)
            ?.takeUnless { it.isArchived || it.teacherId != teacherId }
            ?: return@withContext null
        val topic = topicDao.get(stored.quiz.topicId)
            ?.takeUnless { it.isArchived || it.teacherId != teacherId }
            ?: return@withContext null
        if (topic.classroomId != classroom.id) return@withContext null
        stored.toDomain(json)
    }

    override suspend fun seedDemoData() = withContext(ioDispatcher) {
        val teacherId = remote.currentTeacherId() ?: return@withContext
        val existing = quizDao.observeActiveForTeacher(teacherId).firstOrNull()
        if (!existing.isNullOrEmpty()) return@withContext
        val now = Clock.System.now()
        val classroomId = "demo-classroom"
        val topicId = "demo-topic"
        val demoClassroom = Classroom(
            id = classroomId,
            teacherId = teacherId,
            name = "Period 1 Algebra",
            grade = "8",
            subject = "Math",
            joinCode = JoinCodeGenerator.generate(),
            createdAt = now,
            updatedAt = now,
            students = emptyList()
        )
        val demoTopic = Topic(
            id = topicId,
            classroomId = classroomId,
            teacherId = teacherId,
            name = "Fractions",
            description = "Unit 1",
            createdAt = now,
            updatedAt = now
        )
        database.withTransaction {
            classroomDao.upsert(
                ClassroomEntity(
                    id = demoClassroom.id,
                    teacherId = teacherId,
                    name = demoClassroom.name,
                    grade = demoClassroom.grade,
                    subject = demoClassroom.subject,
                    joinCode = demoClassroom.joinCode,
                    createdAt = now.toEpochMilliseconds(),
                    updatedAt = now.toEpochMilliseconds(),
                    isArchived = false,
                    archivedAt = null,
                    students = emptyList()
                )
            )
            topicDao.upsert(
                TopicEntity(
                    id = demoTopic.id,
                    classroomId = demoTopic.classroomId,
                    teacherId = teacherId,
                    name = demoTopic.name,
                    description = demoTopic.description,
                    createdAt = now.toEpochMilliseconds(),
                    updatedAt = now.toEpochMilliseconds(),
                    isArchived = false,
                    archivedAt = null
                )
            )
        }
        classroomRemote.upsertClassroom(demoClassroom)
        topicRemote.upsertTopic(demoTopic)
        val demoQuiz = Quiz(
            id = generateLocalQuizId(),
            teacherId = teacherId,
            classroomId = classroomId,
            topicId = topicId,
            title = "Fractions Fundamentals",
            defaultTimePerQ = 30,
            shuffle = true,
            createdAt = now,
            category = QuizCategory.STANDARD,
            updatedAt = now,
            questions = (1..10).map { index ->
                Question(
                    id = "demo-q$index",
                    quizId = "demo",
                    type = if (index % 2 == 0) QuestionType.MCQ else QuestionType.TF,
                    stem = "Solve the fraction challenge #$index",
                    choices = listOf("1/2", "1/3", "2/3", "3/4"),
                    answerKey = listOf(if (index % 2 == 0) "1/2" else "True"),
                    explanation = "Use equivalence to compare fractions.",
                    media = if (index == 1) MediaAsset(MediaType.IMAGE, "https://example.com/fractions.png") else null,
                    timeLimitSeconds = 45
                )
            }
        )
        upsert(demoQuiz)
    }

    private fun generateLocalQuizId(): String = "local-${UUID.randomUUID()}"
    private fun shouldIgnorePermissionDenied(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    private fun isTransient(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error?.cause is java.net.UnknownHostException

    private suspend fun hasPendingOps(): Boolean =
        opLogDao.pending(limit = 1).isNotEmpty()
}
