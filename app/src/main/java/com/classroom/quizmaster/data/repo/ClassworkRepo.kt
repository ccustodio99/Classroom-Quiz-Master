package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.model.Classwork
import com.classroom.quizmaster.data.model.Submission
import kotlinx.coroutines.flow.Flow

interface ClassworkRepo {
    fun assignments(classPath: String): Flow<List<Classwork>>
    suspend fun listOnce(classPath: String): List<Classwork>
    suspend fun upsert(classwork: Classwork)
    suspend fun submit(submission: Submission)
}
