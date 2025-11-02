package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.ClassroomRepository
import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.domain.model.ClassroomStatus
import kotlinx.coroutines.flow.Flow

class ClassroomAgentImpl(
    private val classroomRepository: ClassroomRepository,
    private val moduleRepository: ModuleRepository
) : ClassroomAgent {
    override fun observeClassrooms(includeArchived: Boolean): Flow<List<ClassroomProfile>> =
        if (includeArchived) {
            classroomRepository.observeAll()
        } else {
            classroomRepository.observeActive()
        }

    override suspend fun fetchClassroom(id: String): ClassroomProfile? = classroomRepository.get(id)

    override suspend fun createOrUpdate(profile: ClassroomProfile): Result<ClassroomProfile> {
        if (profile.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Class name is required"))
        }
        val normalized = profile.copy(
            name = profile.name.trim(),
            subject = profile.subject.trim(),
            description = profile.description.trim(),
            gradeLevel = profile.gradeLevel.trim(),
            section = profile.section.trim()
        )
        classroomRepository.upsert(normalized)
        return Result.success(normalized)
    }

    override suspend fun setArchived(classroomId: String, archived: Boolean): Result<Unit> {
        val existing = classroomRepository.get(classroomId)
            ?: return Result.failure(IllegalArgumentException("Classroom not found"))
        val status = if (archived) ClassroomStatus.Archived else ClassroomStatus.Active
        val updated = existing.copy(
            status = status,
            archivedAt = if (archived) System.currentTimeMillis() else null
        )
        classroomRepository.upsert(updated)
        val modules = moduleRepository.listModulesByClassroom(classroomId, includeArchived = true)
        modules.forEach { module -> moduleRepository.setArchived(module.id, archived) }
        return Result.success(Unit)
    }
}
