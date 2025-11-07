package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.dao.QuizWithQuestions
import com.classroom.quizmaster.data.local.entity.QuestionEntity
import com.classroom.quizmaster.data.local.entity.QuizEntity
import com.classroom.quizmaster.data.remote.FirebaseQuizDataSource
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.repository.QuizRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val firebaseQuizDataSource: FirebaseQuizDataSource,
    private val database: QuizMasterDatabase,
    private val json: Json
) : QuizRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    override val quizzes: StateFlow<List<Quiz>> = _quizzes.asStateFlow()

    override suspend fun refresh() {
        val remote = firebaseQuizDataSource.loadQuizzes()
        val now = Clock.System.now().toEpochMilliseconds()
        remote.forEach { quiz ->
            database.quizDao().upsertQuizWithQuestions(
                quiz.toEntity(now),
                quiz.questions.mapIndexed { index, question ->
                    question.toEntity(quiz.id, index)
                }
            )
        }
    }

    override suspend fun upsert(quiz: Quiz) {
        val now = Clock.System.now().toEpochMilliseconds()
        val resolvedId = quiz.id.takeIf { it.isNotBlank() } ?: "local-$now"
        val localQuiz = if (quiz.id == resolvedId) quiz else quiz.copy(id = resolvedId)
        database.quizDao().upsertQuizWithQuestions(
            localQuiz.toEntity(now, resolvedId),
            localQuiz.questions.mapIndexed { index, question ->
                question.toEntity(resolvedId, index)
            }
        )
        firebaseQuizDataSource.upsertQuiz(localQuiz)
        if (quiz.id.isBlank()) {
            refresh()
        }
    }

    override suspend fun delete(id: String) {
        firebaseQuizDataSource.deleteQuiz(id)
        database.quizDao().deleteQuiz(id)
    }

    override suspend fun seedDemoData() {
        if (_quizzes.value.isNotEmpty()) return
        val sampleQuiz = Quiz(
            id = "",
            teacherId = "demo-teacher",
            title = "Fractions Fundamentals",
            defaultTimePerQ = 30,
            shuffle = true,
            createdAt = Clock.System.now(),
            questions = (1..10).map { index ->
                Question(
                    id = "q$index",
                    quizId = "demo_quiz",
                    type = if (index % 2 == 0) QuestionType.MCQ else QuestionType.TF,
                    stem = "Question $index: Solve the fraction scenario.",
                    choices = listOf("1/2", "1/3", "2/3", "3/4"),
                    answerKey = listOf(if (index % 2 == 0) "1/2" else "True"),
                    explanation = "Fractions refresher explanation for $index."
                )
            }
        )
        upsert(sampleQuiz)
    }

    init {
        val quizDao = database.quizDao()
        scope.launch {
            quizDao.observeQuizzes().collectLatest { stored ->
                _quizzes.value = stored.map { it.toDomain() }
            }
        }
        scope.launch { refresh() }
    }

    private fun Quiz.toEntity(now: Long, resolvedId: String = id) = QuizEntity(
        id = resolvedId,
        teacherId = teacherId,
        title = title,
        defaultTimePerQ = defaultTimePerQ,
        shuffle = shuffle,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = now
    )

    private fun Question.toEntity(quizId: String, index: Int) = QuestionEntity(
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

    private fun QuizWithQuestions.toDomain(): Quiz = Quiz(
        id = quiz.id,
        teacherId = quiz.teacherId,
        title = quiz.title,
        defaultTimePerQ = quiz.defaultTimePerQ,
        shuffle = quiz.shuffle,
        createdAt = Instant.fromEpochMilliseconds(quiz.createdAt),
        questions = questions
            .sortedBy { it.position }
            .map { it.toDomain(quiz.id) }
    )

    private fun QuestionEntity.toDomain(quizId: String): Question = Question(
        id = id,
        quizId = quizId,
        type = QuestionType.valueOf(type.uppercase()),
        stem = stem,
        choices = json.decodeFromString<List<String>>(choicesJson),
        answerKey = json.decodeFromString<List<String>>(answerKeyJson),
        explanation = explanation,
        media = mediaType?.let { typeName ->
            com.classroom.quizmaster.domain.model.MediaAsset(
                type = com.classroom.quizmaster.domain.model.MediaType.valueOf(typeName.uppercase()),
                url = mediaUrl.orEmpty()
            )
        },
        timeLimitSeconds = timeLimitSeconds
    )
}
