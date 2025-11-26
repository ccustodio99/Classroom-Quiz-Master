package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.LearningMaterial
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.FieldValue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Singleton
class FirebaseMaterialDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun materials() = firestore.collection("materials")

    suspend fun fetchForTeacher(teacherId: String): Result<List<LearningMaterial>> = runCatching {
        materials()
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(FirestoreMaterial::class.java)?.toDomain(doc.id) }
    }

    suspend fun fetchForClassrooms(classroomIds: List<String>): Result<List<LearningMaterial>> = runCatching {
        val normalized = classroomIds.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) return@runCatching emptyList()
        val results = mutableListOf<LearningMaterial>()
        normalized.chunked(10).forEach { chunk ->
            val snapshot = materials()
                .whereIn("classroomId", chunk)
                .get()
                .await()
            results += snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestoreMaterial::class.java)?.toDomain(doc.id)
            }
        }
        results
    }

    suspend fun upsertMaterial(material: LearningMaterial): Result<String> = runCatching {
        val document = if (material.id.isBlank()) materials().document() else materials().document(material.id)
        document.set(FirestoreMaterial.fromDomain(material)).await()
        document.id
    }

    suspend fun archiveMaterial(id: String, archivedAt: Instant): Result<Unit> = runCatching {
        materials()
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
    }

    suspend fun deleteMaterial(id: String): Result<Unit> = runCatching {
        materials().document(id).delete().await()
        Unit
    }

    private data class FirestoreMaterial(
        val teacherId: String = "",
        val classroomId: String = "",
        val classroomName: String = "",
        val topicId: String = "",
        val topicName: String = "",
        val title: String = "",
        val description: String = "",
        val body: String = "",
        val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
        val updatedAt: Long = createdAt,
        // Bind directly to the Firestore field name to avoid duplicate "is" getters on boolean properties.
        @field:PropertyName("isArchived")
        @JvmField
        var archivedFlag: Boolean = false,
        // Legacy field kept to read older documents without warnings.
        @field:PropertyName("archived")
        @JvmField
        var archivedLegacy: Boolean? = null,
        val archivedAt: Long? = null
    ) {
        fun toDomain(id: String): LearningMaterial = LearningMaterial(
            id = id,
            teacherId = teacherId,
            classroomId = classroomId,
            classroomName = classroomName,
            topicId = topicId,
            topicName = topicName,
            title = title,
            description = description,
            body = body,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            isArchived = archivedFlag || (archivedLegacy ?: false),
            archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
        )

        companion object {
            fun fromDomain(material: LearningMaterial) = FirestoreMaterial(
                teacherId = material.teacherId,
                classroomId = material.classroomId,
                classroomName = material.classroomName,
                topicId = material.topicId,
                topicName = material.topicName,
                title = material.title,
                description = material.description,
                body = material.body,
                createdAt = material.createdAt.toEpochMilliseconds(),
                updatedAt = material.updatedAt.toEpochMilliseconds(),
                archivedFlag = material.isArchived,
                archivedAt = material.archivedAt?.toEpochMilliseconds()
            )
        }
    }
}
