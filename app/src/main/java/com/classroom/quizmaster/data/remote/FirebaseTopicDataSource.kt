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

    suspend fun fetchTopics(): Result<List<Topic>> = runCatching {
        val uid = authDataSource.currentUserId() ?: return@runCatching emptyList()
        topicsCollection()
            .whereEqualTo("teacherId", uid)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(FirestoreTopic::class.java)?.toDomain(doc.id)
            }
    }

    suspend fun upsertTopic(topic: Topic): Result<String> = runCatching {
        val document = if (topic.id.isBlank()) topicsCollection().document() else topicsCollection().document(topic.id)
        document.set(FirestoreTopic.fromDomain(topic)).await()
        document.id
    }

    suspend fun archiveTopic(id: String, archivedAt: Instant): Result<Unit> = runCatching {
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
        Unit
    }.onFailure { Timber.e(it, "Failed to archive topic $id") }

    private data class FirestoreTopic(
        val classroomId: String = "",
        val teacherId: String = "",
        val name: String = "",
        val description: String = "",
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val updatedAt: Long = createdAt,
        val isArchived: Boolean = false,
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
            isArchived = isArchived,
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
