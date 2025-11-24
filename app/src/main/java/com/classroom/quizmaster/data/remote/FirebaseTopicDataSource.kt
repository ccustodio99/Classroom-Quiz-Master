package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Topic
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTopicDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authDataSource: FirebaseAuthDataSource
) {

    private fun topicsCollection() = firestore.collection("topics")

    suspend fun fetchTopics(): Result<List<Topic>> {
        val teacherId = authDataSource.currentUserId() ?: return Result.success(emptyList())
        return fetchTopicsForTeacher(teacherId)
    }

    suspend fun fetchTopicsForTeacher(teacherId: String): Result<List<Topic>> {
        return try {
            val documents = topicsCollection()
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(FirestoreTopic::class.java)?.toDomain(doc.id)
                }
            Result.success(documents)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun fetchTopicsForClassrooms(classroomIds: List<String>): Result<List<Topic>> {
        return try {
            val normalized = classroomIds.filter { it.isNotBlank() }.distinct()
            if (normalized.isEmpty()) {
                Result.success(emptyList())
            } else {
                val documents = mutableListOf<Topic>()
                normalized.chunked(10).forEach { chunk ->
                    val snapshot = topicsCollection()
                        .whereIn("classroomId", chunk)
                        .get()
                        .await()
                    documents += snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FirestoreTopic::class.java)?.toDomain(doc.id)
                    }
                }
                Result.success(documents)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun upsertTopic(topic: Topic): Result<String> = try {
        val document = if (topic.id.isBlank()) topicsCollection().document() else topicsCollection().document(topic.id)
        document.set(FirestoreTopic.fromDomain(topic)).await()
        Result.success(document.id)
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun archiveTopic(id: String, archivedAt: Instant): Result<Unit> = try {
        topicsCollection()
            .document(id)
            .update(
                mapOf(
                    "isArchived" to true,
                    "archivedAt" to archivedAt.toEpochMilliseconds(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
        Result.success(Unit)
    } catch (error: Exception) {
        Timber.e(error, "Failed to archive topic $id")
        Result.failure(error)
    }

    private data class FirestoreTopic(
        val classroomId: String = "",
        val teacherId: String = "",
        val name: String = "",
        val description: String = "",
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val updatedAt: Long = createdAt,
        val isArchived: Boolean = false,
        // Legacy field name kept for backward compatibility with old documents.
        val archived: Boolean? = null,
        val archivedAt: Long? = null
    ) {
        fun toDomain(id: String): Topic = Topic(
            id = id,
            classroomId = classroomId,
            teacherId = teacherId,
            name = name,
            description = description,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            isArchived = isArchived || (archived ?: false),
            archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
        )

        companion object {
            fun fromDomain(topic: Topic) = FirestoreTopic(
                classroomId = topic.classroomId,
                teacherId = topic.teacherId,
                name = topic.name,
                description = topic.description,
                createdAt = topic.createdAt.toEpochMilliseconds(),
                updatedAt = topic.updatedAt.toEpochMilliseconds(),
                isArchived = topic.isArchived,
                archivedAt = topic.archivedAt?.toEpochMilliseconds()
            )
        }
    }
}
