package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.AttemptDao
import com.classroom.quizmaster.data.local.AttemptEntity
import com.classroom.quizmaster.domain.model.Attempt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class AttemptRepositoryImpl(
    private val dao: AttemptDao,
    private val json: Json
) : AttemptRepository {
    override suspend fun upsert(attempt: Attempt) {
        val payload = json.encodeToString(Attempt.serializer(), attempt)
        dao.insert(
            AttemptEntity(
                id = attempt.id,
                moduleId = attempt.moduleId,
                assessmentId = attempt.assessmentId,
                studentId = attempt.student.id,
                studentName = attempt.student.displayName,
                attemptJson = payload,
                createdAt = attempt.startedAt
            )
        )
    }

    override suspend fun find(attemptId: String): Attempt? {
        val entity = dao.get(attemptId) ?: return null
        return runCatching { json.decodeFromString(Attempt.serializer(), entity.attemptJson) }.getOrNull()
    }

    override suspend fun findByAssessmentAndStudent(assessmentId: String, studentId: String): Attempt? {
        val entity = dao.getByAssessmentAndStudent(assessmentId, studentId) ?: return null
        return runCatching { json.decodeFromString(Attempt.serializer(), entity.attemptJson) }.getOrNull()
    }

    override fun observeByModule(moduleId: String): Flow<List<Attempt>> {
        return dao.observeByModule(moduleId).map { entities ->
            entities.mapNotNull { entity ->
                runCatching { json.decodeFromString(Attempt.serializer(), entity.attemptJson) }.getOrNull()
            }
        }
    }
}
