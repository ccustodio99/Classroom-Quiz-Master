package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.ClassroomDao
import com.classroom.quizmaster.data.local.ClassroomEntity
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.domain.model.ClassroomStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ClassroomRepositoryImpl(
    private val dao: ClassroomDao,
    private val json: Json
) : ClassroomRepository {
    override fun observeAll(): Flow<List<ClassroomProfile>> =
        dao.observeAll().map { entities -> entities.mapNotNull { it.toDomain(json) } }

    override fun observeActive(): Flow<List<ClassroomProfile>> =
        dao.observeByStatus(ClassroomStatus.Active.name).map { entities ->
            entities.mapNotNull { it.toDomain(json) }
        }

    override suspend fun get(id: String): ClassroomProfile? =
        dao.get(id)?.toDomain(json)

    override suspend fun upsert(profile: ClassroomProfile) {
        val existing = dao.get(profile.id)
        val now = System.currentTimeMillis()
        val createdAt = when {
            profile.createdAt > 0L -> profile.createdAt
            existing != null -> existing.createdAt
            else -> now
        }
        val archivedAt = when {
            profile.status == ClassroomStatus.Archived -> profile.archivedAt ?: now
            else -> null
        }
        val updatedProfile = profile.copy(
            createdAt = createdAt,
            updatedAt = now,
            archivedAt = archivedAt,
            status = if (profile.status == ClassroomStatus.Archived && archivedAt != null) {
                ClassroomStatus.Archived
            } else {
                ClassroomStatus.Active
            }
        )
        val payload = json.encodeToString(ClassroomProfile.serializer(), updatedProfile)
        dao.upsert(
            ClassroomEntity(
                id = updatedProfile.id,
                ownerId = updatedProfile.ownerId,
                name = updatedProfile.name,
                subject = updatedProfile.subject,
                description = updatedProfile.description,
                gradeLevel = updatedProfile.gradeLevel,
                section = updatedProfile.section,
                status = updatedProfile.status.name,
                profileJson = payload,
                createdAt = createdAt,
                updatedAt = now,
                archivedAt = archivedAt
            )
        )
    }
}

private fun ClassroomEntity.toDomain(json: Json): ClassroomProfile? =
    runCatching { json.decodeFromString(ClassroomProfile.serializer(), profileJson) }
        .getOrNull()
        ?.copy(
            createdAt = createdAt,
            updatedAt = updatedAt,
            archivedAt = archivedAt,
            status = runCatching { ClassroomStatus.valueOf(status) }.getOrElse { ClassroomStatus.Active }
        )
