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
    @Query(
        "SELECT * FROM quizzes " +
            "WHERE teacherId = :teacherId AND isArchived = 0 " +
            "ORDER BY createdAt DESC"
    )
    fun observeActiveForTeacher(teacherId: String): Flow<List<QuizWithQuestions>>

    @Transaction
    @Query(
        "SELECT * FROM quizzes " +
            "WHERE classroomId IN (:classroomIds) AND isArchived = 0 " +
            "ORDER BY createdAt DESC"
    )
    fun observeActiveForClassrooms(classroomIds: List<String>): Flow<List<QuizWithQuestions>>

    @Transaction
    @Query("SELECT * FROM quizzes WHERE id = :id LIMIT 1")
    suspend fun getQuiz(id: String): QuizWithQuestions?

    @Query("SELECT * FROM questions WHERE id = :questionId LIMIT 1")
    suspend fun getQuestion(questionId: String): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuiz(quiz: QuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE quizId = :quizId")
    suspend fun deleteQuestionsForQuiz(quizId: String)

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
