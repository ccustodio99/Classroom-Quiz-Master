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
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
        if (assignments.isEmpty()) return flowOf(emptyMap())
        val mappedFlows = assignments.map { assignment ->
            assignmentRepository.submissions(assignment.id)
                .map { submissions -> assignment.id to submissions }
        }
        return mappedFlows.fold(flowOf(emptyMap())) { acc, flow ->
            combine(acc, flow) { current, (assignmentId, submissions) ->
                current + (assignmentId to submissions)
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
        val upcomingOrOpen = mutableListOf<Pair<AssignmentCardUi, Instant>>()
        val closedOrArchived = mutableListOf<Pair<AssignmentCardUi, Instant>>()
        assignments.forEach { assignment ->
            val quiz = quizLookup[assignment.quizId]
            val title = quiz?.title?.ifBlank { "Untitled quiz" } ?: "Untitled quiz"
            val submissionCount = submissions[assignment.id]?.size ?: 0
            val dueInstant = assignment.closeAt
            val statusLabel = when {
                assignment.isArchived -> "Archived"
                now < assignment.openAt -> "Scheduled"
                dueInstant > now -> "Open"
                else -> "Closed"
            }
            val card = AssignmentCardUi(
                id = assignment.id,
                title = title,
                dueIn = formatDue(now, assignment.openAt, dueInstant),
                submissions = submissionCount,
                attemptsAllowed = assignment.attemptsAllowed,
                statusLabel = statusLabel
            )
            val bucket = if (!assignment.isArchived && dueInstant > now) {
                upcomingOrOpen
            } else {
                closedOrArchived
            }
            bucket += card to dueInstant
        }
        return AssignmentsUiState(
            pending = upcomingOrOpen
                .sortedBy { it.second }
                .map { it.first },
            archived = closedOrArchived
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
        val median = medianScore(scores)
        val topicLookup = topics.associateBy { it.id }
        val topicAverages = assignments
            .mapNotNull { assignment ->
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
        val reportRows = assignments
            .sortedByDescending { it.updatedAt }
            .mapNotNull { assignment ->
                val quiz = quizzes.firstOrNull { it.id == assignment.quizId } ?: return@mapNotNull null
                val assignmentScores = submissions[assignment.id].orEmpty()
                val correctRate = if (assignmentScores.isEmpty()) {
                    0f
                } else {
                    assignmentScores
                        .map { it.bestScore.coerceIn(0, 100) / 100f }
                        .average()
                        .toFloat()
                }
                ReportRowUi(
                    question = quiz.title.ifBlank { "Untitled quiz" },
                    pValue = correctRate.coerceIn(0f, 1f),
                    topDistractor = "n/a",
                    distractorRate = 0f
                )
            }
        val lastUpdatedInstant = assignments.maxOfOrNull { it.updatedAt }
        return ReportsUiState(
            average = average,
            median = median,
            topTopics = topicAverages.take(3),
            questionRows = reportRows,
            lastUpdated = lastUpdatedInstant?.let(::formatRelativeTime) ?: "Not updated"
        )
    }

    private fun formatDue(now: Instant, openAt: Instant, closeAt: Instant): String =
        when {
            now < openAt -> "Opens in ${formatDuration(openAt - now)}"
            closeAt > now -> "Due in ${formatDuration(closeAt - now)}"
            else -> "Closed ${formatDuration(now - closeAt)} ago"
        }

    private fun formatDuration(duration: Duration): String = when {
        duration < 1.minutes -> "<1 min"
        duration < 1.hours -> "${duration.inWholeMinutes} min"
        duration < 1.days -> "${duration.inWholeHours} hr"
        else -> "${duration.inWholeDays} d"
    }

    private fun medianScore(scores: List<Int>): Int {
        if (scores.isEmpty()) return 0
        val sorted = scores.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToInt()
        } else {
            sorted[mid]
        }
    }

    private fun formatRelativeTime(instant: Instant): String {
        val now = Clock.System.now()
        val delta = (now - instant).coerceAtLeast(Duration.ZERO)
        return when {
            delta < 1.minutes -> "just now"
            delta < 1.hours -> "${delta.inWholeMinutes} min ago"
            delta < 1.days -> "${delta.inWholeHours} hr ago"
            else -> "${delta.inWholeDays} d ago"
        }
    }

    private fun defaultReportsState() = ReportsUiState(lastUpdated = "Not updated")
}
