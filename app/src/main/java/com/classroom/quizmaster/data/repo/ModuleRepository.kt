package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.model.Module
import kotlinx.coroutines.flow.Flow

interface ModuleRepository {
    fun observeModules(): Flow<List<Module>>
    suspend fun getModule(id: String): Module?
    suspend fun findModuleByAssessment(assessmentId: String): Module?
    suspend fun upsert(module: Module)
}
