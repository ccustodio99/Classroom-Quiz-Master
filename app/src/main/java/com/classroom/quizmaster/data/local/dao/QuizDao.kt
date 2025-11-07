package com.classroom.quizmaster.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.classroom.quizmaster.data.local.entity.QuestionEntity
import com.classroom.quizmaster.data.local.entity.QuizEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    @Transaction
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun observeQuizzes(): Flow<List<QuizWithQuestions>>

    @Transaction
    @Query("SELECT * FROM quizzes WHERE id = :id LIMIT 1")
    suspend fun getQuiz(id: String): QuizWithQuestions?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuiz(quiz: QuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE quizId = :quizId")
    suspend fun deleteQuestionsForQuiz(quizId: String)

    @Query("DELETE FROM quizzes WHERE id = :id")
    suspend fun deleteQuiz(id: String)

    @Transaction
    suspend fun upsertQuizWithQuestions(quiz: QuizEntity, questions: List<QuestionEntity>) {
        upsertQuiz(quiz)
        deleteQuestionsForQuiz(quiz.id)
        if (questions.isNotEmpty()) {
            upsertQuestions(questions)
        }
    }
}

data class QuizWithQuestions(
    @Embedded val quiz: QuizEntity,
    @Relation(parentColumn = "id", entityColumn = "quizId")
    val questions: List<QuestionEntity>
)
