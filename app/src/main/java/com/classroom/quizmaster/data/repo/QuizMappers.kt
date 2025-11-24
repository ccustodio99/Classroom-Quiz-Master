package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.dao.QuizWithQuestions
import com.classroom.quizmaster.data.local.entity.QuestionEntity
import com.classroom.quizmaster.data.local.entity.QuizEntity
import com.classroom.quizmaster.domain.model.MediaAsset
import com.classroom.quizmaster.domain.model.MediaType
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.QuizCategory
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun Quiz.toEntity(timestamp: Instant = Clock.System.now(), resolvedId: String = id): QuizEntity =
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

fun Question.toEntity(
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

fun QuizWithQuestions.toDomain(json: Json): Quiz = Quiz(
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

fun QuestionEntity.toDomain(json: Json, quizId: String): Question = Question(
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

