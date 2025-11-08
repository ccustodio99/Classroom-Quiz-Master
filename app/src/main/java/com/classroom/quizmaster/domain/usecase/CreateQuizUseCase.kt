package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.MediaAsset
import com.classroom.quizmaster.domain.model.MediaType
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.repository.MediaRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.datetime.Clock

class CreateQuizUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
    private val mediaRepository: MediaRepository
) {

    data class MediaUpload(
        val questionId: String,
        val mediaType: MediaType,
        val fileName: String,
        val bytes: ByteArray,
        val mimeType: String
    )

    suspend operator fun invoke(
        quiz: Quiz,
        mediaUploads: List<MediaUpload> = emptyList()
    ): Quiz {
        require(quiz.teacherId.isNotBlank()) { "Teacher id is required" }
        require(quiz.title.isNotBlank()) { "Quiz title cannot be blank" }
        require(quiz.questions.isNotEmpty()) { "Quiz must contain at least one question" }

        val resolvedId = quiz.id.ifBlank { "quiz-${UUID.randomUUID()}" }
        val sanitizedQuestions = quiz.questions.map { question ->
            require(question.stem.isNotBlank()) { "Question stem cannot be blank" }
            val normalizedAnswers = question.answerKey.map(String::trim).filter(String::isNotEmpty)
            require(normalizedAnswers.isNotEmpty()) { "Question ${question.id} must include an answer" }
            val normalizedChoices = question.choices.map(String::trim).filter(String::isNotEmpty)
            if (question.type == QuestionType.MCQ) {
                require(normalizedChoices.size >= 2) { "Multiple choice questions require at least two choices" }
                require(normalizedAnswers.all { normalizedChoices.contains(it) }) {
                    "Answer key must be present in the provided choices"
                }
            }
            val boundedTime = question.timeLimitSeconds.coerceIn(5, 600)
            question.copy(
                stem = question.stem.trim(),
                choices = if (normalizedChoices.isEmpty()) question.choices else normalizedChoices,
                answerKey = normalizedAnswers,
                timeLimitSeconds = boundedTime
            )
        }

        val sanitizedQuiz = quiz.copy(
            id = resolvedId,
            title = quiz.title.trim(),
            questions = sanitizedQuestions,
            questionCount = sanitizedQuestions.size,
            updatedAt = Clock.System.now()
        )

        val uploadsByQuestion = mediaUploads.groupBy(MediaUpload::questionId)
        val resolvedQuestions = sanitizedQuiz.questions.map { question ->
            val uploads = uploadsByQuestion[question.id].orEmpty()
            if (uploads.isEmpty()) {
                question
            } else {
                val upload = uploads.first()
                val mediaUrl = mediaRepository.uploadQuizAsset(
                    quizId = sanitizedQuiz.id,
                    fileName = upload.fileName,
                    bytes = upload.bytes,
                    mimeType = upload.mimeType
                ).getOrElse { throw IllegalStateException("Unable to upload media for ${question.id}", it) }
                question.copy(media = MediaAsset(type = upload.mediaType, url = mediaUrl))
            }
        }

        val quizWithMedia = sanitizedQuiz.copy(questions = resolvedQuestions)
        quizRepository.upsert(quizWithMedia)
        return quizWithMedia
    }
}
