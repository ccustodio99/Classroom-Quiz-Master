package com.classroom.quizmaster.ui.state

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.ui.model.ReportRowUi
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsUiState
import com.classroom.quizmaster.ui.teacher.reports.ReportsUiState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Singleton
class RealAssignmentRepositoryUi @Inject constructor(
    private val assignmentRepository: AssignmentRepository,
    private val quizRepository: QuizRepository,
    private val classroomRepository: ClassroomRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AssignmentRepositoryUi {

    override val assignments: Flow<AssignmentsUiState> =
        assignmentRepository.assignments
            .flatMapLatest { assignments ->
                if (assignments.isEmpty()) {
                    flowOf(AssignmentsUiState())
                } else {
                    combine(
                        submissionMapFlow(assignments),
                        quizRepository.quizzes
                    ) { submissions, quizzes ->
                        buildAssignmentsState(assignments, submissions, quizzes)
                    }
                }
            }
            .onStart { emit(AssignmentsUiState()) }
            .distinctUntilChanged()
            .flowOn(dispatcher)

    override val reports: Flow<ReportsUiState> =
        assignmentRepository.assignments
            .flatMapLatest { assignments ->
                if (assignments.isEmpty()) {
                    flowOf(defaultReportsState())
                } else {
                    combine(
                        submissionMapFlow(assignments),
                        quizRepository.quizzes,
                        classroomRepository.topics
                    ) { submissions, quizzes, topics ->
                        buildReportsState(assignments, submissions, quizzes, topics)
                    }
                }
            }
            .onStart { emit(defaultReportsState()) }
            .distinctUntilChanged()
            .flowOn(dispatcher)

    private fun submissionMapFlow(assignments: List<Assignment>): Flow<Map<String, List<Submission>>> {
        val flows = assignments.map { assignment ->
            assignmentRepository.submissions(assignment.id)
        }
        return if (flows.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(flows) { submissionLists ->
                submissionLists.mapIndexed { index, submissions ->
                    assignments[index].id to submissions
                }.toMap()
            }
        }
    }

    private fun buildAssignmentsState(
        assignments: List<Assignment>,
        submissions: Map<String, List<Submission>>,
        quizzes: List<Quiz>
    ): AssignmentsUiState {
        val quizLookup = quizzes.associateBy { it.id }
        val now = Clock.System.now()
        val pending = mutableListOf<Pair<AssignmentCardUi, Instant>>()
        val archived = mutableListOf<Pair<AssignmentCardUi, Instant>>()
        assignments.forEach { assignment ->
            val quiz = quizLookup[assignment.quizId]
            val title = quiz?.title?.ifBlank { "Untitled quiz" } ?: "Untitled quiz"
            val submissionCount = submissions[assignment.id]?.size ?: 0
            val dueInstant = assignment.closeAt
            val statusLabel = if (dueInstant > now) "Open" else "Closed"
            val card = AssignmentCardUi(
                id = assignment.id,
                title = title,
                dueIn = formatDue(now, dueInstant),
                submissions = submissionCount,
                total = submissionCount,
                statusLabel = statusLabel
            )
            val bucket = if (dueInstant > now) pending else archived
            bucket += card to dueInstant
        }
        return AssignmentsUiState(
            pending = pending
                .sortedBy { it.second }
                .map { it.first },
            archived = archived
                .sortedByDescending { it.second }
                .map { it.first }
        )
    }

    private fun buildReportsState(
        assignments: List<Assignment>,
        submissions: Map<String, List<Submission>>,
        quizzes: List<Quiz>,
        topics: List<Topic>
    ): ReportsUiState {
        val scores = submissions.values.flatten().map { it.bestScore }
        val average = if (scores.isEmpty()) 0 else scores.average().roundToInt()
        val median = if (scores.isEmpty()) 0 else scores.sorted()[scores.size / 2]
        val topicLookup = topics.associateBy { it.id }
        val topicAverages = assignments.mapNotNull { assignment ->
            val topic = topicLookup[assignment.topicId] ?: return@mapNotNull null
            val topicScores = submissions[assignment.id].orEmpty().map { it.bestScore }
            if (topicScores.isEmpty()) return@mapNotNull null
            topic.id to topicScores.average()
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.average() }
            .toList()
            .sortedByDescending { it.second }
            .mapNotNull { (topicId, _) -> topicLookup[topicId]?.name }
        val reportRows = assignments.mapNotNull { assignment ->
            val quiz = quizzes.firstOrNull { it.id == assignment.quizId } ?: return@mapNotNull null
            val assignmentScores = submissions[assignment.id].orEmpty()
            val correctRate = if (assignmentScores.isEmpty()) {
                0f
            } else {
                assignmentScores.map { it.bestScore / 100f }.average().toFloat()
            }
            ReportRowUi(
                question = quiz.title.ifBlank { "Untitled quiz" },
                pValue = correctRate.coerceIn(0f, 1f),
                topDistractor = "—",
                distractorRate = 0f
            )
        }
        val lastUpdated = assignments.maxOfOrNull { it.updatedAt } ?: Clock.System.now()
        return ReportsUiState(
            average = average,
            median = median,
            topTopics = topicAverages.take(3),
            questionRows = reportRows,
            lastUpdated = formatRelativeTime(lastUpdated)
        )
    }

    private fun formatDue(now: Instant, due: Instant): String {
        val delta = due - now
        return if (delta.isNegative()) {
            val elapsed = -delta
            when {
                elapsed < 1.minutes -> "closed just now"
                elapsed < 1.hours -> "closed ${elapsed.inWholeMinutes} min ago"
                elapsed < 1.days -> "closed ${elapsed.inWholeHours} hr ago"
                else -> "closed ${elapsed.inWholeDays} d ago"
            }
        } else {
            when {
                delta < 1.minutes -> "due in <1 min"
                delta < 1.hours -> "due in ${delta.inWholeMinutes} min"
                delta < 1.days -> "due in ${delta.inWholeHours} hr"
                else -> "due in ${delta.inWholeDays} d"
            }
        }
    }

    private fun formatRelativeTime(instant: Instant): String {
        val now = Clock.System.now()
        val delta = now - instant
        return when {
            delta < 1.minutes -> "just now"
            delta < 1.hours -> "${delta.inWholeMinutes} min ago"
            delta < 1.days -> "${delta.inWholeHours} hr ago"
            else -> "${delta.inWholeDays} d ago"
        }
    }

    private fun defaultReportsState() = ReportsUiState()
}
