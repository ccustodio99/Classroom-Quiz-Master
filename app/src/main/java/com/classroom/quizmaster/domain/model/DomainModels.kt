package com.classroom.quizmaster.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("admin")
    Admin,
    @SerialName("teacher")
    Teacher,
    @SerialName("student")
    Student,
    @SerialName("guardian")
    Guardian,
    @SerialName("pending")
    Pending,
    @SerialName("blocked")
    Blocked
}

@Serializable
enum class AccountStatus {
    Active,
    Inactive,
    PendingApproval
}

@Serializable
data class UserAccount(
    val id: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
    val status: AccountStatus,
    val hashedPassword: Long,
    val createdAt: Long,
    val approvedAt: Long? = null,
    val approvedBy: String? = null,
    val lastLoginAt: Long? = null
)

@Serializable
data class Student(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val classroomId: String? = null
)

@Serializable
data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val acquiredAt: Long
)

@Serializable
data class PersonaBlueprint(
    val id: String,
    val title: String,
    val description: String,
    val role: UserRole,
    val defaultPermissions: List<String> = emptyList(),
    val enabled: Boolean = true
)

@Serializable
enum class JoinPolicy {
    @SerialName("code")
    CODE,
    @SerialName("invite_only")
    INVITE_ONLY,
    @SerialName("organization")
    ORGANIZATION
}

@Serializable
enum class ClassroomStatus { Active, Archived }

@Serializable
data class ClassroomProfile(
    val id: String = "classroom-default",
    val code: String = "000000",
    val name: String = "Advisory Class",
    val subject: String = "G11 General Mathematics",
    val description: String = "",
    val gradeLevel: String = "Grade 11",
    val section: String = "",
    val ownerId: String? = null,
    val coTeacherIds: List<String> = emptyList(),
    val joinPolicy: JoinPolicy = JoinPolicy.CODE,
    val orgId: String? = null,
    val status: ClassroomStatus = ClassroomStatus.Active,
    val settings: ClassroomSettings = ClassroomSettings(),
    val stream: List<ClassStreamItem> = emptyList(),
    val topics: List<ClassworkTopic> = emptyList(),
    val classwork: List<ClassworkItem> = emptyList(),
    val roster: List<ClassRosterEntry> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val archivedAt: Long? = null
)

@Serializable
data class ClassroomSettings(
    val allowStudentPosts: Boolean = true,
    val enableGuardianEmails: Boolean = false,
    val gradingCategories: List<String> = listOf("Assessment", "Practice", "Participation"),
    val defaultWeighting: Map<String, Double> = mapOf(
        "Assessment" to 0.5,
        "Practice" to 0.3,
        "Participation" to 0.2
    ),
    val leaderboardEnabled: Boolean = false,
    val presenceHeartbeatMs: Long = 30_000L
)

@Serializable
enum class ClassroomRole {
    Teacher,
    Student,
    Guardian
}

@Serializable
data class ClassRosterEntry(
    val userId: String,
    val role: ClassroomRole,
    val displayName: String,
    val joinedAt: Long,
    val guardianEmail: String? = null,
    val muted: Boolean = false
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
enum class ClassworkStatus {
    Draft,
    Published,
    Archived
}

@Serializable
data class ClassworkTopic(
    val id: String,
    val title: String,
    val position: Int = 0
)

@Serializable
data class ClassworkItem(
    val id: String,
    val classId: String,
    val topicId: String?,
    val title: String,
    val summary: String = "",
    val type: ClassworkType,
    val moduleId: String? = null,
    val assignmentId: String? = null,
    val liveSessionId: String? = null,
    val dueAt: Long? = null,
    val points: Int? = null,
    val attachments: List<String> = emptyList(),
    val status: ClassworkStatus = ClassworkStatus.Draft,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
enum class StreamItemType {
    Announcement,
    ClassworkPosted,
    Reminder,
    LiveNow
}

@Serializable
data class ClassStreamItem(
    val id: String,
    val classId: String,
    val type: StreamItemType,
    val headline: String,
    val message: String? = null,
    val authorId: String,
    val pinned: Boolean = false,
    val classworkId: String? = null,
    val createdAt: Long
)

@Serializable
enum class LiveSessionMode {
    Individual,
    Team,
    Anonymous
}

@Serializable
enum class LiveSessionStatus {
    Scheduled,
    InProgress,
    Completed,
    Aborted
}

@Serializable
data class LiveSessionMeta(
    val id: String,
    val classId: String,
    val moduleId: String,
    val hostId: String,
    val assignmentId: String? = null,
    val mode: LiveSessionMode = LiveSessionMode.Individual,
    val status: LiveSessionStatus = LiveSessionStatus.Scheduled,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val lanSessionId: String? = null,
    val fallbackChannel: String? = null,
    val participantCount: Int = 0
)

@Serializable
data class Module(
    val id: String,
    val classroom: ClassroomProfile = ClassroomProfile(),
    val subject: String = classroom.subject,
    val topic: String,
    val objectives: List<String>,
    val preTest: Assessment,
    val lesson: Lesson,
    val postTest: Assessment,
    val settings: ModuleSettings,
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class ModuleSettings(
    val allowLeaderboard: Boolean = false,
    val revealAnswersAfterSection: Boolean = true,
    val timePerItemSeconds: Int = 60
)

@Serializable
data class Lesson(
    val id: String,
    val slides: List<LessonSlide>,
    val interactiveActivities: List<InteractiveActivity> = emptyList(),
    val topics: List<LessonTopic> = emptyList()
)

@Serializable
data class LessonTopic(
    val id: String,
    val name: String,
    val learningObjectives: List<String>,
    val details: String,
    val materials: List<LearningMaterial> = emptyList(),
    val preTest: Assessment,
    val postTest: Assessment,
    val interactiveAssessments: List<InteractiveActivity> = emptyList(),
    val archived: Boolean = false
)

@Serializable
enum class LearningMaterialType {
    Document,
    Presentation,
    Spreadsheet,
    Media,
    Link,
    Other
}

@Serializable
data class LearningMaterial(
    val id: String,
    val title: String,
    val type: LearningMaterialType,
    val reference: String
)

@Serializable
data class LessonSlide(
    val id: String,
    val title: String,
    val content: String,
    val miniCheck: MiniCheck? = null
)

@Serializable
data class MiniCheck(
    val prompt: String,
    val correctAnswer: String,
    val objectives: List<String> = emptyList()
)

@Serializable
sealed interface InteractiveActivity {
    val id: String
    val title: String
    val prompt: String
    val isScored: Boolean
}

@Serializable
@kotlinx.serialization.SerialName("quiz")
data class QuizActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val options: List<String>,
    val correctAnswers: List<Int>,
    val allowMultiple: Boolean = false,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("true_false")
data class TrueFalseActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val correctAnswer: Boolean,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("type_answer")
data class TypeAnswerActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val correctAnswer: String,
    val maxCharacters: Int = 20,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("puzzle")
data class PuzzleActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val blocks: List<String>,
    val correctOrder: List<String>,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("slider")
data class SliderActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val minValue: Int,
    val maxValue: Int,
    val target: Int,
    override val isScored: Boolean = true
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("poll")
data class PollActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val options: List<String>,
    override val isScored: Boolean = false
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("word_cloud")
data class WordCloudActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val maxWords: Int = 1,
    val maxCharacters: Int = 16,
    override val isScored: Boolean = false
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("open_ended")
data class OpenEndedActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val maxCharacters: Int = 240,
    override val isScored: Boolean = false
) : InteractiveActivity

@Serializable
@kotlinx.serialization.SerialName("brainstorm")
data class BrainstormActivity(
    override val id: String,
    override val title: String,
    override val prompt: String,
    val categories: List<String>,
    val voteLimit: Int = 2,
    override val isScored: Boolean = false
) : InteractiveActivity

@Serializable
sealed interface Item {
    val id: String
    val objective: String
    val prompt: String
    val explanation: String
}

@Serializable
data class MultipleChoiceItem(
    override val id: String,
    override val objective: String,
    override val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    override val explanation: String
) : Item

@Serializable
data class TrueFalseItem(
    override val id: String,
    override val objective: String,
    override val prompt: String,
    val answer: Boolean,
    override val explanation: String
) : Item

@Serializable
data class NumericItem(
    override val id: String,
    override val objective: String,
    override val prompt: String,
    val answer: Double,
    val tolerance: Double = 0.01,
    override val explanation: String
) : Item

@Serializable
data class MatchingPair(
    val left: String,
    val right: String
)

@Serializable
data class MatchingItem(
    override val id: String,
    override val objective: String,
    override val prompt: String,
    val pairs: List<MatchingPair>,
    override val explanation: String
) : Item

@Serializable
data class Assessment(
    val id: String,
    val items: List<Item>,
    val timePerItemSec: Int = 60
)

@Serializable
data class Attempt(
    val id: String,
    val assessmentId: String,
    val student: Student,
    val startedAt: Long,
    val submittedAt: Long? = null,
    val responses: List<AttemptItemResult>? = null
)

@Serializable
data class AttemptItemResult(
    val itemId: String,
    val answer: String,
    val isCorrect: Boolean,
    val score: Double,
    val maxScore: Double = 1.0
)

@Serializable
data class AttemptSummary(
    val student: Student,
    val prePercent: Double?,
    val postPercent: Double?
)

@Serializable
data class Scorecard(
    val attemptId: String,
    val totalScore: Double,
    val maxScore: Double,
    val percent: Double,
    val objectiveBreakdown: Map<String, ObjectiveScore>
)

@Serializable
data class ObjectiveScore(
    val objective: String,
    val earned: Double,
    val total: Double
)

@Serializable
data class Assignment(
    val id: String,
    val moduleId: String,
    val dueAt: Long
)

@Serializable
data class ClassReport(
    val moduleId: String,
    val topic: String,
    val preAverage: Double,
    val postAverage: Double,
    val objectiveMastery: Map<String, ObjectiveMastery>,
    val attempts: List<AttemptSummary>
)

@Serializable
data class StudentReport(
    val moduleId: String,
    val student: Student,
    val preScore: Double,
    val postScore: Double,
    val mastery: Map<String, ObjectiveMastery>
)

@Serializable
data class ObjectiveMastery(
    val objective: String,
    val pre: Double,
    val post: Double
)

@Serializable
data class CourseSummary(
    val id: String,
    val title: String,
    val subject: String,
    val description: String,
    val gradeLevel: String,
    val imageUrl: String,
    val totalModules: Int,
    val difficulty: CourseDifficulty
)

@Serializable
enum class CourseDifficulty {
    Beginner,
    Intermediate,
    Advanced
}

@Serializable
enum class HomeFeedType {
    Assigned,
    Trending,
    Recommended,
    RecentlyViewed
}

@Serializable
data class HomeFeedItem(
    val id: String,
    val type: HomeFeedType,
    val task: HomeTask
)

@Serializable
data class HomeTask(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val progress: Float
)

@Serializable
enum class LearningUnitType {
    Course,
    Module,
    Lesson,
    Practice,
    Assessment
}

@Serializable
data class LearningUnit(
    val id: String,
    val title: String,
    val type: LearningUnitType,
    val totalItems: Int,
    val completedItems: Int,
    val deeplink: String
)

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
data class Roster(
    val classId: String,
    val userId: String,
    val role: ClassroomRole
)

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
