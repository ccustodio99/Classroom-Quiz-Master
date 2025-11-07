package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.remote.FirebaseQuizDataSource
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.repository.QuizRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val firebaseQuizDataSource: FirebaseQuizDataSource
) : QuizRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    override val quizzes: StateFlow<List<Quiz>> = _quizzes.asStateFlow()

    override suspend fun refresh() {
        _quizzes.value = firebaseQuizDataSource.loadQuizzes()
    }

    override suspend fun upsert(quiz: Quiz) {
        firebaseQuizDataSource.upsertQuiz(quiz)
        refresh()
    }

    override suspend fun delete(id: String) {
        firebaseQuizDataSource.deleteQuiz(id)
        refresh()
    }

    override suspend fun seedDemoData() {
        if (_quizzes.value.isNotEmpty()) return
        val sampleQuiz = Quiz(
            id = "",
            teacherId = "demo-teacher",
            title = "Fractions Fundamentals",
            defaultTimePerQ = 30,
            shuffle = true,
            createdAt = Clock.System.now(),
            questions = (1..10).map { index ->
                Question(
                    id = "q$index",
                    quizId = "demo_quiz",
                    type = if (index % 2 == 0) QuestionType.MCQ else QuestionType.TF,
                    stem = "Question $index: Solve the fraction scenario.",
                    choices = listOf("1/2", "1/3", "2/3", "3/4"),
                    answerKey = listOf(if (index % 2 == 0) "1/2" else "True"),
                    explanation = "Fractions refresher explanation for $index."
                )
            }
        )
        upsert(sampleQuiz)
    }

    init {
        scope.launch { refresh() }
    }
}
