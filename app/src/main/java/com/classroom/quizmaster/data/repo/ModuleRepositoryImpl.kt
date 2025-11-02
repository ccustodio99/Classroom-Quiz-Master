package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.ModuleDao
import com.classroom.quizmaster.data.local.ModuleEntity
import com.classroom.quizmaster.domain.model.Lesson
import com.classroom.quizmaster.domain.model.Module
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ModuleRepositoryImpl(
    private val dao: ModuleDao,
    private val json: Json
) : ModuleRepository {
    override fun observeModules(includeArchived: Boolean): Flow<List<Module>> {
        val source = if (includeArchived) dao.observeAllModules() else dao.observeActiveModules()
        return source.map { entities -> entities.mapNotNull { it.toDomain(json) } }
    }

    override fun observeModulesByClassroom(
        classroomId: String,
        includeArchived: Boolean
    ): Flow<List<Module>> {
        return dao.observeByClassroom(classroomId, includeArchived).map { entities ->
            entities.mapNotNull { it.toDomain(json) }
        }
    }

    override suspend fun getModule(id: String): Module? {
        val entity = dao.getModule(id) ?: return null
        return entity.toDomain(json)
    }

    override suspend fun listModules(includeArchived: Boolean): List<Module> {
        val entities = if (includeArchived) dao.getAll() else dao.getActive()
        return entities.mapNotNull { it.toDomain(json) }
    }

    override suspend fun listModulesByClassroom(
        classroomId: String,
        includeArchived: Boolean
    ): List<Module> {
        return dao.getByClassroom(classroomId, includeArchived).mapNotNull { it.toDomain(json) }
    }

    override suspend fun findModuleByAssessment(assessmentId: String): Module? {
        val entity = dao.getByAssessmentId(assessmentId) ?: return null
        val module = entity.toDomain(json) ?: return null
        return if (module.preTest.id == assessmentId || module.postTest.id == assessmentId) module else null
    }

    override suspend fun findLesson(lessonId: String): Lesson? {
        val entity = dao.getByLessonId(lessonId) ?: return null
        val module = entity.toDomain(json) ?: return null
        return module.lesson.takeIf { it.id == lessonId }
    }

    override suspend fun upsert(module: Module) {
        val existing = dao.getModule(module.id)
        val now = System.currentTimeMillis()
        val createdAt = when {
            module.createdAt > 0L -> module.createdAt
            existing != null -> existing.createdAt
            else -> now
        }
        val updatedModule = module.copy(
            archived = module.archived,
            createdAt = createdAt,
            updatedAt = now
        )
        val jsonValue = json.encodeToString(Module.serializer(), updatedModule)
        dao.insert(
            ModuleEntity(
                id = updatedModule.id,
                moduleJson = jsonValue,
                preAssessmentId = updatedModule.preTest.id,
                postAssessmentId = updatedModule.postTest.id,
                lessonId = updatedModule.lesson.id,
                classroomId = updatedModule.classroom.id.ifBlank { null },
                archived = updatedModule.archived,
                createdAt = createdAt,
                updatedAt = now
            )
        )
    }

    override suspend fun setArchived(moduleId: String, archived: Boolean) {
        val entity = dao.getModule(moduleId) ?: return
        val module = entity.toDomain(json) ?: return
        val updatedAt = System.currentTimeMillis()
        val updatedModule = module.copy(archived = archived, updatedAt = updatedAt)
        val payload = json.encodeToString(Module.serializer(), updatedModule)
        dao.insert(
            entity.copy(
                moduleJson = payload,
                archived = archived,
                updatedAt = updatedAt
            )
        )
    }
}

private fun ModuleEntity.toDomain(json: Json): Module? =
    runCatching { json.decodeFromString(Module.serializer(), moduleJson) }
        .getOrNull()
        ?.let { module ->
            val created = if (module.createdAt > 0L) module.createdAt else createdAt
            val updated = if (module.updatedAt > 0L) module.updatedAt else updatedAt
            module.copy(
                archived = archived,
                createdAt = created,
                updatedAt = updated
            )
        }
