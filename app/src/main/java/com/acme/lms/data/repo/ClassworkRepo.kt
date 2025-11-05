package com.acme.lms.data.repo

import com.acme.lms.data.model.Classwork
import com.acme.lms.data.model.Submission
import kotlinx.coroutines.flow.Flow

interface ClassworkRepo {
    fun assignments(classPath: String): Flow<List<Classwork>>
    suspend fun listOnce(classPath: String): List<Classwork>
    suspend fun upsert(classwork: Classwork)
    suspend fun submit(submission: Submission)
}
