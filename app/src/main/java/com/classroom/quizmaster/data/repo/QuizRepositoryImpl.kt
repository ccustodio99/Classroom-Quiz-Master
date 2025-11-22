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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : QuizRepository {

    private val quizDao = database.quizDao()
    private val classroomDao = database.classroomDao()
    private val topicDao = database.topicDao()

    override val quizzes: Flow<List<Quiz>> =
        authRepository.authState
            .switchMapLatest { auth ->
                val teacherId = auth.userId
                if (teacherId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        quizDao.observeActiveForTeacher(teacherId),
                        classroomDao.observeForTeacher(teacherId),
                        topicDao.observeForTeacher(teacherId)
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
                }
            }
            .distinctUntilChanged()

    override suspend fun refresh() = withContext(ioDispatcher) {
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
                    Timber.w(err, "Skipping remote quiz upsert due to permissions")
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
                    Timber.w(err, "Skipping remote quiz archive for $id")
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

    private fun Quiz.toEntity(timestamp: Instant = Clock.System.now(), resolvedId: String = id): QuizEntity =
        QuizEntity(
            id = resolvedId,
            teacherId = teacherId,
            classroomId = classroomId,
            topicId = topicId,
            title = title,
            defaultTimePerQ = defaultTimePerQ,
            shuffle = shuffle,
            questionCount = questionCount.takeIf { it > 0 } ?: questions.size,
            category = category.name,
            createdAt = createdAt.toEpochMilliseconds(),
            updatedAt = timestamp.toEpochMilliseconds(),
            isArchived = isArchived,
            archivedAt = archivedAt?.toEpochMilliseconds()
        )

    private fun Question.toEntity(
        quizId: String,
        index: Int,
        json: Json
    ): QuestionEntity = QuestionEntity(
        id = id.ifBlank { "${quizId}-q$index" },
        quizId = quizId,
        type = type.name,
        stem = stem,
        choicesJson = json.encodeToString(choices),
        answerKeyJson = json.encodeToString(answerKey),
        explanation = explanation,
        mediaType = media?.type?.name,
        mediaUrl = media?.url,
        timeLimitSeconds = timeLimitSeconds,
        position = index,
        updatedAt = Clock.System.now().toEpochMilliseconds()
    )

    private fun QuizWithQuestions.toDomain(json: Json): Quiz = Quiz(
        id = quiz.id,
        teacherId = quiz.teacherId,
        classroomId = quiz.classroomId,
        topicId = quiz.topicId,
        title = quiz.title,
        defaultTimePerQ = quiz.defaultTimePerQ,
        shuffle = quiz.shuffle,
        createdAt = Instant.fromEpochMilliseconds(quiz.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(quiz.updatedAt),
        questionCount = quiz.questionCount,
        questions = questions
            .sortedBy { it.position }
            .map { it.toDomain(json, quiz.id) },
        category = runCatching { QuizCategory.valueOf(quiz.category) }.getOrDefault(QuizCategory.STANDARD),
        isArchived = quiz.isArchived,
        archivedAt = quiz.archivedAt?.let(Instant::fromEpochMilliseconds)
    )

    private fun QuestionEntity.toDomain(json: Json, quizId: String): Question = Question(
        id = id,
        quizId = quizId,
        type = QuestionType.valueOf(type.uppercase()),
        stem = stem,
        choices = json.decodeFromString(choicesJson),
        answerKey = json.decodeFromString(answerKeyJson),
        explanation = explanation,
        media = mediaType?.let { typeValue ->
            MediaAsset(
                type = MediaType.valueOf(typeValue.uppercase()),
                url = mediaUrl.orEmpty()
            )
        },
        timeLimitSeconds = timeLimitSeconds
    )

    private fun generateLocalQuizId(): String = "local-${UUID.randomUUID()}"
    private fun shouldIgnorePermissionDenied(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    private fun isTransient(error: Throwable?): Boolean =
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error?.cause is java.net.UnknownHostException
}
