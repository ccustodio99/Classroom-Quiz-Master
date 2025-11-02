package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.model.ClassroomProfile
import kotlinx.coroutines.flow.Flow

interface ClassroomRepository {
    fun observeAll(): Flow<List<ClassroomProfile>>
    fun observeActive(): Flow<List<ClassroomProfile>>
    suspend fun get(id: String): ClassroomProfile?
    suspend fun upsert(profile: ClassroomProfile)
}
