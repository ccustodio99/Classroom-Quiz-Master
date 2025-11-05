package com.acme.lms.data.repo

import com.example.lms.core.model.Classwork
import com.example.lms.core.model.Submission
import kotlinx.coroutines.flow.Flow

interface ClassworkRepo {
    fun assignments(classPath: String): Flow<List<Classwork>>
    suspend fun listOnce(classPath: String): List<Classwork>
    suspend fun upsert(classwork: Classwork)
    suspend fun submit(submission: Submission)
}
