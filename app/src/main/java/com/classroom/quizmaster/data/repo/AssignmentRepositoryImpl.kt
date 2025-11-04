package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.AssignmentDao
import com.classroom.quizmaster.data.local.AssignmentEntity
import com.classroom.quizmaster.domain.model.Assignment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class AssignmentRepositoryImpl(
    private val dao: AssignmentDao,
    private val json: Json
) : AssignmentRepository {
    override suspend fun upsert(assignment: Assignment) {
        val payload = json.encodeToString(Assignment.serializer(), assignment)
        dao.insert(
            AssignmentEntity(
                id = assignment.id,
                moduleId = assignment.moduleId,
                assignmentJson = payload
            )
        )
    }

    override suspend fun get(id: String): Assignment? {
        val entity = dao.get(id) ?: return null
        return entity.toDomain(json)
    }

    override fun observeAll(): Flow<List<Assignment>> {
        return dao.observeAll().map { entities ->
            entities.mapNotNull { it.toDomain(json) }
        }
    }

    override fun observeForModule(moduleId: String): Flow<List<Assignment>> {
        return dao.observeForModule(moduleId).map { entities ->
            entities.mapNotNull { it.toDomain(json) }
        }
    }

    override fun observeAssignment(id: String): Flow<Assignment?> {
        return dao.observeById(id).map { entity -> entity?.toDomain(json) }
    }
}

private fun AssignmentEntity.toDomain(json: Json): Assignment? =
    runCatching { json.decodeFromString(Assignment.serializer(), assignmentJson) }.getOrNull()
