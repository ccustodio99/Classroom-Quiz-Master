package com.classroom.quizmaster.ui.teacher.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class QuizEditorViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    fun saveQuiz(title: String, defaultTime: Int, shuffle: Boolean, onSaved: () -> Unit) {
        val quizId = UUID.randomUUID().toString()
        val quiz = Quiz(
            id = quizId,
            teacherId = firebaseAuth.currentUser?.uid ?: "local-teacher",
            title = title.ifBlank { "Untitled Quiz" },
            defaultTimePerQ = defaultTime,
            shuffle = shuffle,
            createdAt = Clock.System.now(),
            questions = listOf(
                Question(
                    id = UUID.randomUUID().toString(),
                    quizId = quizId,
                    type = QuestionType.MCQ,
                    stem = "What fraction equals 0.5?",
                    choices = listOf("1/2", "1/3", "3/4", "2/5"),
                    answerKey = listOf("1/2"),
                    explanation = "1 divided by 2 equals 0.5"
                )
            )
        )
        viewModelScope.launch {
            quizRepository.upsert(quiz)
            onSaved()
        }
    }
}
