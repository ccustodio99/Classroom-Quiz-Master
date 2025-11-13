package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.Quiz
import com.google.firebase.firestore.FirebaseFirestore
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

    fun currentTeacherId(): String? = authDataSource.currentUserId()

    suspend fun loadQuizzes(): List<Quiz> = runCatching {
        val teacherId = currentTeacherId() ?: return emptyList()
        quizCollection()
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(FirestoreQuiz::class.java)?.toDomain(doc.id, json)
            }
    }.onFailure { Timber.e(it, "Failed to load quizzes") }
        .getOrDefault(emptyList())

    suspend fun upsertQuiz(quiz: Quiz): Result<Unit> = runCatching {
        val document = if (quiz.id.isBlank()) quizCollection().document() else quizCollection().document(quiz.id)
        document.set(FirestoreQuiz.fromDomain(quiz, json), SetOptions.merge()).await()
        Unit
    }.onFailure { Timber.e(it, "Failed to upsert quiz") }

    suspend fun deleteQuiz(id: String): Result<Unit> = runCatching {
        quizCollection().document(id).delete().await()
        Unit
    }.onFailure { Timber.e(it, "Failed to delete quiz") }

    data class FirestoreQuiz(
        val teacherId: String = "",
        val title: String = "",
        val defaultTimePerQ: Int = 30,
        val shuffle: Boolean = false,
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val updatedAt: Long = createdAt,
        val questionCount: Int = 0,
        val questionsJson: String = "[]"
    ) {
        fun toDomain(id: String, json: Json): Quiz {
            val decodedQuestions: List<Question> = json.decodeFromString(questionsJson)
            val computedCount = if (questionCount > 0) questionCount else decodedQuestions.size
            return Quiz(
                id = id,
                teacherId = teacherId,
                title = title,
                defaultTimePerQ = defaultTimePerQ,
                shuffle = shuffle,
                createdAt = Instant.fromEpochMilliseconds(createdAt),
                updatedAt = Instant.fromEpochMilliseconds(updatedAt),
                questionCount = computedCount,
                questions = decodedQuestions
            )
        }

        companion object {
            fun fromDomain(quiz: Quiz, json: Json) = FirestoreQuiz(
                teacherId = quiz.teacherId,
                title = quiz.title,
                defaultTimePerQ = quiz.defaultTimePerQ,
                shuffle = quiz.shuffle,
                createdAt = quiz.createdAt.toEpochMilliseconds(),
                updatedAt = quiz.updatedAt.toEpochMilliseconds(),
                questionCount = if (quiz.questionCount > 0) quiz.questionCount else quiz.questions.size,
                questionsJson = json.encodeToString(quiz.questions)
            )
        }
    }
}
