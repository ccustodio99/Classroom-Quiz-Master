package com.classroom.quizmaster.data.repo

import androidx.room.withTransaction
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.dao.QuizWithQuestions
import com.classroom.quizmaster.data.local.entity.QuestionEntity
import com.classroom.quizmaster.data.local.entity.QuizEntity
import com.classroom.quizmaster.data.remote.FirebaseQuizDataSource
import com.classroom.quizmaster.domain.model.MediaAsset
import com.classroom.quizmaster.domain.model.MediaType
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.repository.QuizRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val remote: FirebaseQuizDataSource,
    private val database: QuizMasterDatabase,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : QuizRepository {

    private val quizDao = database.quizDao()

    override val quizzes: Flow<List<Quiz>> =
        quizDao.observeQuizzes()
            .map { stored -> stored.map { it.toDomain(json) } }
            .distinctUntilChanged()

    override suspend fun refresh() = withContext(ioDispatcher) {
        val remoteQuizzes = remote.loadQuizzes()
        if (remoteQuizzes.isEmpty()) return@withContext
        database.withTransaction {
            remoteQuizzes.forEach { quiz ->
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
        val normalized = if (quiz.id == resolvedId) quiz else quiz.copy(id = resolvedId)
        database.withTransaction {
            quizDao.upsertQuizWithQuestions(
                normalized.toEntity(now, resolvedId),
                normalized.questions.mapIndexed { index, question ->
                    question.toEntity(resolvedId, index, json)
                }
            )
        }
        remote.upsertQuiz(normalized)
            .onFailure { Timber.e(it, "Failed to upsert quiz ${normalized.id}") }
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        remote.deleteQuiz(id)
            .onFailure { Timber.e(it, "Failed to delete quiz $id remotely") }
        database.withTransaction { quizDao.deleteQuiz(id) }
    }

    override suspend fun getQuiz(id: String): Quiz? = withContext(ioDispatcher) {
        quizDao.getQuiz(id)?.toDomain(json)
    }

    override suspend fun seedDemoData() = withContext(ioDispatcher) {
        val teacherId = remote.currentTeacherId() ?: return@withContext
        val existing = quizDao.observeQuizzes().firstOrNull()
        if (!existing.isNullOrEmpty()) return@withContext
        val now = Clock.System.now()
        val demoQuiz = Quiz(
            id = generateLocalQuizId(),
            teacherId = teacherId,
            title = "Fractions Fundamentals",
            defaultTimePerQ = 30,
            shuffle = true,
            createdAt = now,
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
            title = title,
            defaultTimePerQ = defaultTimePerQ,
            shuffle = shuffle,
            questionCount = questionCount.takeIf { it > 0 } ?: questions.size,
            createdAt = createdAt.toEpochMilliseconds(),
            updatedAt = timestamp.toEpochMilliseconds()
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
        title = quiz.title,
        defaultTimePerQ = quiz.defaultTimePerQ,
        shuffle = quiz.shuffle,
        createdAt = Instant.fromEpochMilliseconds(quiz.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(quiz.updatedAt),
        questionCount = quiz.questionCount,
        questions = questions
            .sortedBy { it.position }
            .map { it.toDomain(json, quiz.id) }
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
}
