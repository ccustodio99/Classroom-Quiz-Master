package com.classroom.quizmaster.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PersonaType {
    @SerialName("learner")
    Learner,
    @SerialName("instructor")
    Instructor,
    @SerialName("admin")
    Admin
}

@Serializable
data class User(
    val id: String,
    val name: String,
    val role: PersonaType,
    val org: String? = null,
    val email: String
)

@Serializable
enum class ClassJoinPolicy {
    @SerialName("open")
    OPEN,
    @SerialName("invite_only")
    INVITE_ONLY
}

@Serializable
data class Class(
    val id: String,
    val code: String,
    val section: String,
    val subject: String,
    val ownerId: String,
    val coTeachers: List<String> = emptyList(),
    val joinPolicy: ClassJoinPolicy = ClassJoinPolicy.INVITE_ONLY
)

@Serializable
enum class ClassRole {
    @SerialName("teacher")
    TEACHER,
    @SerialName("student")
    STUDENT
}

@Serializable
data class Roster(
    val classId: String,
    val userId: String,
    val role: ClassRole
)

@Serializable
enum class ClassworkType {
    @SerialName("material")
    MATERIAL,
    @SerialName("quiz")
    QUIZ,
    @SerialName("pretest")
    PRETEST,
    @SerialName("posttest")
    POSTTEST,
    @SerialName("discussion")
    DISCUSSION,
    @SerialName("live")
    LIVE
}

@Serializable
data class Classwork(
    val id: String,
    val classId: String,
    val topic: String? = null,
    val type: ClassworkType,
    val title: String,
    val dueAt: Long? = null,
    val points: Int? = null
)

@Serializable
data class Submission(
    val id: String,
    val classworkId: String,
    val userId: String,
    val submittedAt: Long,
    val grade: Float? = null,
    val attachments: List<FileRef> = emptyList()
)

@Serializable
sealed class Question {
    abstract val id: String
    abstract val prompt: String
    abstract val mediaUrl: String?
}

@Serializable
@SerialName("multiple_choice")
data class MultipleChoiceQuestion(
    override val id: String,
    override val prompt: String,
    override val mediaUrl: String? = null,
    val options: List<String>,
    val correctAnswers: List<Int>, // indices of correct options
    val allowMultiple: Boolean = false
) : Question()

@Serializable
@SerialName("true_false")
data class TrueFalseQuestion(
    override val id: String,
    override val prompt: String,
    override val mediaUrl: String? = null,
    val correctAnswer: Boolean
) : Question()

@Serializable
@SerialName("short_answer")
data class ShortAnswerQuestion(
    override val id: String,
    override val prompt: String,
    override val mediaUrl: String? = null,
    val correctAnswer: String,
    val tolerance: Double? = null // For numeric answers
) : Question()

@Serializable
data class Attempt(
    val id: String,
    val assessmentId: String, // could be classworkId
    val userId: String,
    val startedAt: Long,
    val completedAt: Long?,
    val answers: Map<String, String> // QuestionId to Answer
)

@Serializable
data class LiveSession(
    val id: String,
    val classworkId: String, // The 'live' classwork item
    val hostId: String,
    val joinCode: String,
    val isActive: Boolean = true,
    val currentQuestionId: String? = null
)

@Serializable
data class LiveResponse(
    val id: String,
    val liveSessionId: String,
    val questionId: String,
    val userId: String,
    val response: String, // This could be a JSON string for complex answers
    val submittedAt: Long,
    val isCorrect: Boolean? = null,
    val score: Int? = null
)

@Serializable
data class FileRef(
    val path: String,
    val mimeType: String
)

@Serializable
data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String
)

@Serializable
data class Certificate(
    val id: String,
    val title: String,
    val description: String,
    val issuedAt: Long,
    val fileRef: FileRef? = null
)

@Serializable
data class ActivityTimeline(
    val streakDays: Int,
    val badges: List<Badge>,
    val certificates: List<Certificate>,
    val lastActiveAt: Long
)
