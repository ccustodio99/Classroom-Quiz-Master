package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.QuizCategory
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseQuizDataSource @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val firestore: FirebaseFirestore,
    private val json: Json
) {

    private fun quizCollection() = firestore.collection("quizzes")

    suspend fun currentTeacherId(): String? = authDataSource.currentUserId()

    suspend fun loadQuizzes(): List<Quiz> {
        val teacherId = currentTeacherId() ?: return emptyList()
        return loadQuizzesForTeacher(teacherId)
    }

    suspend fun loadQuizzesForTeacher(teacherId: String): List<Quiz> {
        return try {
            quizCollection()
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(FirestoreQuiz::class.java)?.toDomain(doc.id, json)
                        ?.takeIf { it.classroomId.isNotBlank() && it.topicId.isNotBlank() }
                }
        } catch (error: Exception) {
            Timber.e(error, "Failed to load quizzes for teacher")
            emptyList()
        }
    }

    suspend fun loadQuizzesByIds(ids: List<String>): List<Quiz> {
        return try {
            val normalized = ids.filter { it.isNotBlank() }.distinct()
            if (normalized.isEmpty()) {
                emptyList()
            } else {
                val documents = mutableListOf<Quiz>()
                normalized.chunked(10).forEach { chunk ->
                    val snapshot = quizCollection()
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    documents += snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FirestoreQuiz::class.java)?.toDomain(doc.id, json)
                    }
                }
                documents
            }
        } catch (error: Exception) {
            Timber.e(error, "Failed to load quizzes for ids")
            emptyList()
        }
    }

    suspend fun upsertQuiz(quiz: Quiz): Result<Unit> = try {
        val document = if (quiz.id.isBlank()) quizCollection().document() else quizCollection().document(quiz.id)
        document.set(FirestoreQuiz.fromDomain(quiz, json), SetOptions.merge()).await()
        Result.success(Unit)
    } catch (error: Exception) {
        Timber.e(error, "Failed to upsert quiz")
        Result.failure(error)
    }

    suspend fun archiveQuiz(id: String, archivedAt: Instant): Result<Unit> = try {
        quizCollection()
            .document(id)
            .set(
                mapOf(
                    "isArchived" to true,
                    "archivedAt" to archivedAt.toEpochMilliseconds(),
                    "updatedAt" to archivedAt.toEpochMilliseconds()
                ),
                SetOptions.merge()
            )
            .await()
        Result.success(Unit)
    } catch (error: Exception) {
        Timber.e(error, "Failed to archive quiz")
        Result.failure(error)
    }

    data class FirestoreQuiz(
        val teacherId: String = "",
        val classroomId: String = "",
        val topicId: String = "",
        val title: String = "",
        val defaultTimePerQ: Int = 30,
        val shuffle: Boolean = false,
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val updatedAt: Long = createdAt,
        val questionCount: Int = 0,
        val questionsJson: String = "[]",
        val category: String = QuizCategory.STANDARD.name,
        @get:PropertyName("isArchived")
        @set:PropertyName("isArchived")
        var archivedFlag: Boolean = false,
        // Legacy field kept to read older documents without warnings.
        @get:PropertyName("archived")
        @set:PropertyName("archived")
        var archivedLegacy: Boolean? = null,
        val archivedAt: Long? = null
    ) {
        fun toDomain(id: String, json: Json): Quiz {
            val decodedQuestions: List<Question> = json.decodeFromString(questionsJson)
            val computedCount = if (questionCount > 0) questionCount else decodedQuestions.size
            return Quiz(
                id = id,
                teacherId = teacherId,
                classroomId = classroomId,
                topicId = topicId,
                title = title,
                defaultTimePerQ = defaultTimePerQ,
                shuffle = shuffle,
                createdAt = Instant.fromEpochMilliseconds(createdAt),
                updatedAt = Instant.fromEpochMilliseconds(updatedAt),
                questionCount = computedCount,
                questions = decodedQuestions,
                category = runCatching { QuizCategory.valueOf(category) }.getOrDefault(QuizCategory.STANDARD),
                isArchived = archivedFlag || (archivedLegacy ?: false),
                archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
            )
        }

        companion object {
            fun fromDomain(quiz: Quiz, json: Json) = FirestoreQuiz(
                teacherId = quiz.teacherId,
                classroomId = quiz.classroomId,
                topicId = quiz.topicId,
                title = quiz.title,
                defaultTimePerQ = quiz.defaultTimePerQ,
                shuffle = quiz.shuffle,
                createdAt = quiz.createdAt.toEpochMilliseconds(),
                updatedAt = quiz.updatedAt.toEpochMilliseconds(),
                questionCount = if (quiz.questionCount > 0) quiz.questionCount else quiz.questions.size,
                questionsJson = json.encodeToString(quiz.questions),
                category = quiz.category.name,
                archivedFlag = quiz.isArchived,
                archivedAt = quiz.archivedAt?.toEpochMilliseconds()
            )
        }
    }
}
