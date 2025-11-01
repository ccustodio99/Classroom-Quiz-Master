package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.ModuleDao
import com.classroom.quizmaster.data.local.ModuleEntity
import com.classroom.quizmaster.domain.model.Lesson
import com.classroom.quizmaster.domain.model.Module
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class ModuleRepositoryImpl(
    private val dao: ModuleDao,
    private val json: Json
) : ModuleRepository {
    override fun observeModules(): Flow<List<Module>> {
        return dao.observeModules().map { entities ->
            entities.mapNotNull { it.toDomainOrNull(json) }
        }
    }

    override suspend fun getModule(id: String): Module? {
        val entity = dao.getModule(id) ?: return null
        return entity.toDomainOrNull(json)
    }

    override suspend fun findModuleByAssessment(assessmentId: String): Module? {
        return dao.getAll().firstNotNullOfOrNull { entity ->
            val module = entity.toDomainOrNull(json) ?: return@firstNotNullOfOrNull null
            if (module.preTest.id == assessmentId || module.postTest.id == assessmentId) module else null
        }
    }

    override suspend fun findLesson(lessonId: String): Lesson? {
        return dao.getAll().firstNotNullOfOrNull { entity ->
            val module = entity.toDomainOrNull(json) ?: return@firstNotNullOfOrNull null
            module.lesson.takeIf { it.id == lessonId }
        }
    }

    override suspend fun upsert(module: Module) {
        val jsonValue = json.encodeToString(Module.serializer(), module)
        dao.insert(ModuleEntity(id = module.id, moduleJson = jsonValue))
    }
}

private fun ModuleEntity.toDomainOrNull(json: Json): Module? =
    runCatching { json.decodeFromString(Module.serializer(), moduleJson) }.getOrNull()
