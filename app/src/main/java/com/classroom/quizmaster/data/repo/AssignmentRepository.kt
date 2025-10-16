package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.model.Assignment
import kotlinx.coroutines.flow.Flow

interface AssignmentRepository {
    suspend fun upsert(assignment: Assignment)
    suspend fun get(id: String): Assignment?
    fun observeForModule(moduleId: String): Flow<List<Assignment>>
    fun observeAssignment(id: String): Flow<Assignment?>
}
