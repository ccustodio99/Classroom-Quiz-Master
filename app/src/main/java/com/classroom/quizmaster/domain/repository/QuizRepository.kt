package com.classroom.quizmaster.domain.repository

import com.classroom.quizmaster.domain.model.Quiz
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    val quizzes: Flow<List<Quiz>>
    suspend fun refresh()
    suspend fun upsert(quiz: Quiz)
    suspend fun delete(id: String)
    suspend fun getQuiz(id: String): Quiz?
    suspend fun seedDemoData()
}
