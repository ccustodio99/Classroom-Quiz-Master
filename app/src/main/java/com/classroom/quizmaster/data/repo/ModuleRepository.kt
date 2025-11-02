package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.model.Lesson
import com.classroom.quizmaster.domain.model.Module
import kotlinx.coroutines.flow.Flow

interface ModuleRepository {
    fun observeModules(includeArchived: Boolean = false): Flow<List<Module>>
    fun observeModulesByClassroom(classroomId: String, includeArchived: Boolean = false): Flow<List<Module>>
    suspend fun getModule(id: String): Module?
    suspend fun listModules(includeArchived: Boolean = false): List<Module>
    suspend fun listModulesByClassroom(classroomId: String, includeArchived: Boolean = false): List<Module>
    suspend fun findModuleByAssessment(assessmentId: String): Module?
    suspend fun findLesson(lessonId: String): Lesson?
    suspend fun upsert(module: Module)
    suspend fun setArchived(moduleId: String, archived: Boolean)
}
