package com.classroom.quizmaster.ui.state

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsUiState
import com.classroom.quizmaster.ui.teacher.reports.AssignmentCompletionUi
import com.classroom.quizmaster.ui.teacher.reports.AssignmentPerformanceUi
import com.classroom.quizmaster.ui.teacher.reports.CompletionOverview
import com.classroom.quizmaster.ui.teacher.reports.QuestionDifficultyTag
import com.classroom.quizmaster.ui.teacher.reports.QuestionDifficultyUi
import com.classroom.quizmaster.ui.teacher.reports.ReportClassroomOption
import com.classroom.quizmaster.ui.teacher.reports.ReportsUiState
import com.classroom.quizmaster.ui.teacher.reports.StudentCompletionUi
import com.classroom.quizmaster.ui.teacher.reports.StudentImprovementUi
import com.classroom.quizmaster.ui.teacher.reports.StudentProgressAtRiskUi
import com.classroom.quizmaster.ui.teacher.reports.StudentProgressUi
import com.classroom.quizmaster.ui.teacher.reports.StudentTrend
import com.classroom.quizmaster.ui.teacher.reports.TopicMasteryUi
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.sqrt
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import com.classroom.quizmaster.util.switchMapLatest
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow

private const val TREND_WINDOW = 5
private const val TREND_DELTA_THRESHOLD = 2.5
private const val MIN_ATTEMPTS_FOR_DIFFICULTY = 3

private data class CompletionAccumulator(
    var completedOnTime: Int = 0,
    var completedLate: Int = 0,
    var notAttempted: Int = 0,
    var totalAssignments: Int = 0
)

@Singleton
class RealAssignmentRepositoryUi @Inject constructor(
    private val assignmentRepository: AssignmentRepository,
    private val quizRepository: QuizRepository,
    private val classroomRepository: ClassroomRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AssignmentRepositoryUi {

    private val selectedReportClassroom = MutableStateFlow<String?>(null)

    override fun selectReportsClassroom(classroomId: String?) {
        selectedReportClassroom.value = classroomId?.takeIf { it.isNotBlank() }
    }

    override val assignments: Flow<AssignmentsUiState> =
        assignmentRepository.assignments
            .switchMapLatest { assignments ->
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
        combine(selectedReportClassroom, assignmentRepository.assignments) { classroomId, assignments ->
            classroomId to assignments.filterByClassroom(classroomId)
        }
            .flatMapLatest { (selectedClassroomId, filteredAssignments) ->
                if (filteredAssignments.isEmpty()) {
                    combine(
                        classroomRepository.classrooms,
                        classroomRepository.topics,
                        classroomRepository.students
                    ) { classrooms, topics, students ->
                        val options = classroomOptions(classrooms, selectedClassroomId)
                        val classroomName = classroomDisplayName(filteredAssignments, classrooms, selectedClassroomId)
                        defaultReportsState(
                            classroomName = classroomName,
                            selectedClassroomId = selectedClassroomId,
                            classroomOptions = options
                        )
                    }
                } else {
                    combine(
                        submissionMapFlow(filteredAssignments),
                        quizRepository.quizzes,
                        classroomRepository.topics,
                        classroomRepository.students,
                        classroomRepository.classrooms
                    ) { submissions, quizzes, topics, students, classrooms ->
                        val options = classroomOptions(classrooms, selectedClassroomId)
                        buildReportsState(
                            filteredAssignments,
                            submissions,
                            quizzes,
                            topics,
                            students,
                            classrooms,
                            selectedClassroomId,
                            options
                        )
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
        topics: List<Topic>,
        students: List<Student>,
        classrooms: List<Classroom>,
        selectedClassroomId: String?,
        classroomOptions: List<ReportClassroomOption>
    ): ReportsUiState {
        val assignmentLookup = assignments.associateBy { it.id }
        val quizLookup = quizzes.associateBy { it.id }
        val studentNameLookup = students.associateBy({ it.id }, { it.displayName.ifBlank { it.email.ifBlank { it.id } } })
        val classroomRoster = classrooms.associateBy({ it.id }, { it.students.toSet() })
        val questionCountLookup = assignments.associate { assignment ->
            val quiz = quizLookup[assignment.quizId]
            assignment.id to questionCountFor(quiz)
        }

        val normalizedScores = submissions.flatMap { (assignmentId, list) ->
            val questionCount = questionCountLookup[assignmentId] ?: 0
            list.map { normalizeScore(it.bestScore, questionCount).roundToInt() }
        }
        val average = if (normalizedScores.isEmpty()) 0 else normalizedScores.average().roundToInt()
        val median = medianScore(normalizedScores)
        val topicLookup = topics.associateBy { it.id }
        val topicAverages = assignments
            .mapNotNull { assignment ->
                val topic = topicLookup[assignment.topicId] ?: return@mapNotNull null
                val quiz = quizLookup[assignment.quizId] ?: return@mapNotNull null
                val questionCount = questionCountFor(quiz)
                val topicScores = submissions[assignment.id].orEmpty().map {
                    normalizeScore(it.bestScore, questionCount)
                }
                if (topicScores.isEmpty()) return@mapNotNull null
                topic.id to topicScores.average()
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.average() }
            .toList()
            .sortedByDescending { it.second }
            .mapNotNull { (topicId, value) ->
                val topic = topicLookup[topicId] ?: return@mapNotNull null
                TopicMasteryUi(
                    topicName = topic.name,
                    averageScore = value.roundToInt()
                )
            }
        val assignmentRows = mutableListOf<AssignmentPerformanceUi>()
        val assignmentCompletionRows = mutableListOf<AssignmentCompletionUi>()
        val questionDifficultyRows = mutableListOf<QuestionDifficultyUi>()
        val studentScores = mutableMapOf<String, MutableList<Pair<Instant, Double>>>()
        val studentCompletion = mutableMapOf<String, CompletionAccumulator>()

        assignments.sortedByDescending { it.updatedAt }.forEach { assignment ->
            val quiz = quizLookup[assignment.quizId] ?: return@forEach
            val submissionsForAssignment = submissions[assignment.id].orEmpty()
            val questionCount = questionCountFor(quiz).takeIf { it > 0 } ?: 1

            if (submissionsForAssignment.isNotEmpty()) {
                val pValuePercent = submissionsForAssignment
                    .map { normalizeScore(it.bestScore, questionCount) }
                    .ifEmpty { listOf(0.0) }
                    .average()
                    .roundToInt()
                assignmentRows += AssignmentPerformanceUi(
                    assignmentId = assignment.id,
                    title = quiz.title.ifBlank { "Untitled quiz" },
                    pValue = pValuePercent.coerceIn(0, 100),
                    topDistractor = "n/a",
                    distractorRate = 0
                )
            }

            val roster = classroomRoster[assignment.classroomId].orEmpty()
            val submissionsByStudent = submissionsForAssignment.groupBy { it.uid }
            val studentIdsInScope = if (roster.isNotEmpty()) roster else submissionsByStudent.keys

            val completedOnTime = submissionsByStudent
                .filterKeys { studentIdsInScope.isEmpty() || it in studentIdsInScope }
                .count { (_, subs) -> subs.any { !it.isLate(assignment) } }
            val completedLate = submissionsByStudent
                .filterKeys { studentIdsInScope.isEmpty() || it in studentIdsInScope }
                .count { (_, subs) -> subs.isNotEmpty() && subs.all { it.isLate(assignment) } }
            val totalStudents = studentIdsInScope.size
            val notStarted = (totalStudents - (completedOnTime + completedLate)).coerceAtLeast(0)

            assignmentCompletionRows += AssignmentCompletionUi(
                assignmentId = assignment.id,
                title = quiz.title.ifBlank { "Untitled quiz" },
                totalStudents = totalStudents,
                completedOnTime = completedOnTime,
                completedLate = completedLate,
                notStarted = notStarted
            )

            studentIdsInScope.forEach { studentId ->
                val accumulator = studentCompletion.getOrPut(studentId) { CompletionAccumulator() }
                accumulator.totalAssignments += 1
                val subs = submissionsByStudent[studentId].orEmpty()
                when {
                    subs.isEmpty() -> accumulator.notAttempted += 1
                    subs.any { !it.isLate(assignment) } -> accumulator.completedOnTime += 1
                    else -> accumulator.completedLate += 1
                }
            }

            submissionsByStudent.forEach { (uid, subs) ->
                val best = subs.maxOfOrNull { it.bestScore } ?: return@forEach
                val normalized = normalizeScore(best, questionCount)
                studentScores.getOrPut(uid) { mutableListOf() }
                    .add(assignment.closeAt to normalized)
            }

            if (submissionsForAssignment.isNotEmpty() && quiz.questions.isNotEmpty()) {
                val normalized = submissionsForAssignment.map { normalizeScore(it.bestScore, questionCount) }
                val avgScore = normalized.average().coerceIn(0.0, 100.0)
                val tag = classifyDifficulty(avgScore, normalized, normalized.size)
                quiz.questions.forEach { question ->
                    questionDifficultyRows += QuestionDifficultyUi(
                        questionId = question.id,
                        assignmentTitle = quiz.title.ifBlank { "Untitled quiz" },
                        questionPreview = question.stem.ifBlank { "Question" },
                        pValue = avgScore.roundToInt().coerceIn(0, 100),
                        topWrongOptionLabel = null,
                        topWrongOptionRate = null,
                        difficultyTag = tag,
                        attemptCount = normalized.size
                    )
                }
            }
        }

        val completionTotalsStudents = assignmentCompletionRows.sumOf { it.totalStudents }
        val completionOverview = if (completionTotalsStudents == 0) {
            CompletionOverview()
        } else {
            CompletionOverview(
                onTimeRate = percent(
                    assignmentCompletionRows.sumOf { it.completedOnTime },
                    completionTotalsStudents
                ),
                lateRate = percent(
                    assignmentCompletionRows.sumOf { it.completedLate },
                    completionTotalsStudents
                ),
                notAttemptedRate = percent(
                    assignmentCompletionRows.sumOf { it.notStarted },
                    completionTotalsStudents
                )
            )
        }

        val studentProgress = studentScores.map { (uid, scores) ->
            val completed = scores.size
            val total = assignments.size.coerceAtLeast(1)
            val score = scores.map { it.second }.ifEmpty { listOf(0.0) }.average().roundToInt()
            StudentProgressUi(
                name = studentNameLookup[uid] ?: uid,
                completed = completed,
                total = total,
                score = score
            )
        }.sortedByDescending { it.score }

        val studentCompletionRows = studentCompletion.map { (uid, counts) ->
            StudentCompletionUi(
                studentId = uid,
                name = studentNameLookup[uid] ?: uid,
                completedOnTime = counts.completedOnTime,
                completedLate = counts.completedLate,
                notAttempted = counts.notAttempted,
                totalAssignments = counts.totalAssignments
            )
        }.sortedWith(
            compareByDescending<StudentCompletionUi> { it.notAttempted }
                .thenByDescending { it.completedLate }
                .thenBy { it.name }
        )

        val studentProgressAtRisk = (studentCompletion.keys + studentScores.keys)
            .distinct()
            .map { uid ->
                val scores = studentScores[uid].orEmpty().sortedBy { it.first }.map { it.second }
                val averageScore = scores.ifEmpty { listOf(0.0) }.average()
                val trend = calculateTrend(scores)
                val counts = studentCompletion[uid]
                val missing = counts?.notAttempted ?: 0
                val completed = counts?.let { it.completedOnTime + it.completedLate } ?: scores.size
                val atRisk = ((averageScore < 70 && completed > 0) || missing >= 2 || trend == StudentTrend.DECLINING)
                StudentProgressAtRiskUi(
                    studentId = uid,
                    name = studentNameLookup[uid] ?: uid,
                    averageScore = averageScore.roundToInt().coerceIn(0, 100),
                    trend = trend,
                    completedCount = completed,
                    missingCount = missing,
                    atRisk = atRisk
                )
            }
            .sortedWith(
                compareByDescending<StudentProgressAtRiskUi> { it.atRisk }
                    .thenBy { it.averageScore }
                    .thenBy { it.name }
            )

        val lastSubmissionUpdate = submissions.values.flatten().maxOfOrNull { it.updatedAt }
        val lastUpdatedInstant = listOfNotNull(
            assignments.maxOfOrNull { it.updatedAt },
            lastSubmissionUpdate
        ).maxOrNull()

        // Pre/Post analysis by student
        val preScoresByUid = mutableMapOf<String, MutableList<Int>>()
        val postScoresByUid = mutableMapOf<String, MutableList<Int>>()
        submissions.forEach { (assignmentId, subs) ->
            val assignment = assignmentLookup[assignmentId] ?: return@forEach
            val quiz = quizLookup[assignment.quizId] ?: return@forEach
            val questionCount = questionCountLookup[assignmentId]?.takeIf { it > 0 }
                ?: questionCountFor(quiz).takeIf { it > 0 }
                ?: 1
            subs.forEach { submission ->
                val normalizedScore = normalizeScore(submission.bestScore, questionCount).roundToInt()
                when (quiz.category) {
                    QuizCategory.PRE_TEST -> preScoresByUid.getOrPut(submission.uid) { mutableListOf() }.add(normalizedScore)
                    QuizCategory.POST_TEST -> postScoresByUid.getOrPut(submission.uid) { mutableListOf() }.add(normalizedScore)
                    else -> {}
                }
            }
        }
        val classPreAverage = preScoresByUid.values.flatten().let { list ->
            if (list.isEmpty()) 0 else list.average().roundToInt()
        }
        val classPostAverage = postScoresByUid.values.flatten().let { list ->
            if (list.isEmpty()) 0 else list.average().roundToInt()
        }
        val classDelta = classPostAverage - classPreAverage

        val studentImprovement = (preScoresByUid.keys + postScoresByUid.keys)
            .distinct()
            .map { uid ->
                val preList = preScoresByUid[uid].orEmpty()
                val postList = postScoresByUid[uid].orEmpty()
                if (preList.isEmpty() || postList.isEmpty()) return@map null
                val preAvg = if (preList.isEmpty()) 0 else preList.average().roundToInt()
                val postAvg = if (postList.isEmpty()) 0 else postList.average().roundToInt()
                StudentImprovementUi(
                    name = studentNameLookup[uid] ?: uid,
                    preAvg = preAvg,
                    postAvg = postAvg,
                    delta = postAvg - preAvg,
                    preAttempts = preList.size,
                    postAttempts = postList.size
                )
            }
            .filterNotNull()
            .sortedByDescending { it.delta }

        val classroomIds = assignments.map { it.classroomId }.distinct().toSet()
        val activeClassrooms = classrooms.filterNot { it.isArchived }
        val relevantClassrooms = activeClassrooms.filter { it.id in classroomIds }
        val classroomName = classroomDisplayName(assignments, classrooms, selectedClassroomId)

        return ReportsUiState(
            isLoading = false,
            classroomName = classroomName,
            lastUpdated = lastUpdatedInstant,
            lastUpdatedLabel = lastUpdatedInstant?.let(::formatRelativeTime) ?: "Not updated yet",
            average = average,
            median = median,
            classPreAverage = classPreAverage,
            classPostAverage = classPostAverage,
            classDelta = classDelta,
            topTopics = topicAverages.take(3),
            assignments = assignmentRows,
            studentProgress = studentProgress,
            studentImprovement = studentImprovement,
            completionOverview = completionOverview,
            assignmentCompletion = assignmentCompletionRows,
            studentCompletion = studentCompletionRows,
            studentProgressAtRisk = studentProgressAtRisk,
            questionDifficulty = questionDifficultyRows,
            selectedClassroomId = selectedClassroomId,
            classroomOptions = classroomOptions
        )
    }

    private fun formatDue(now: Instant, openAt: Instant, closeAt: Instant): String =
        when {
            now < openAt -> "Opens in ${formatDuration(openAt - now)}"
            closeAt > now -> "Due in ${formatDuration(closeAt - now)}"
            else -> "Closed ${formatDuration(now - closeAt)} ago"
        }

    private fun normalizeScore(bestScore: Int, questionCount: Int): Double {
        if (questionCount <= 0) return bestScore.toDouble().coerceAtLeast(0.0)
        val treatedAsPercent = if (bestScore <= questionCount) {
            (bestScore.toDouble() / questionCount) * 100.0
        } else {
            bestScore.toDouble()
        }
        return treatedAsPercent.coerceIn(0.0, 100.0)
    }

    private fun Submission.isLate(assignment: Assignment): Boolean = updatedAt > assignment.closeAt

    private fun questionCountFor(quiz: Quiz?): Int =
        quiz?.questionCount?.takeIf { it > 0 } ?: quiz?.questions?.size ?: 0

    private fun percent(part: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((part.toDouble() / total.toDouble()) * 100).roundToInt().coerceIn(0, 100)
    }

    private fun calculateTrend(scores: List<Double>): StudentTrend {
        val windowed = scores.takeLast(TREND_WINDOW)
        if (windowed.size < 2) return StudentTrend.UNKNOWN
        val deltas = windowed.zipWithNext { previous, current -> current - previous }
        val avgDelta = deltas.average()
        return when {
            avgDelta > TREND_DELTA_THRESHOLD -> StudentTrend.IMPROVING
            avgDelta < -TREND_DELTA_THRESHOLD -> StudentTrend.DECLINING
            else -> StudentTrend.STABLE
        }
    }

    private fun classifyDifficulty(pValue: Double, scores: List<Double>, attemptCount: Int): QuestionDifficultyTag {
        if (attemptCount < MIN_ATTEMPTS_FOR_DIFFICULTY) return QuestionDifficultyTag.INSUFFICIENT_DATA
        return when {
            pValue >= 90 -> QuestionDifficultyTag.TOO_EASY
            pValue <= 30 -> QuestionDifficultyTag.TOO_HARD
            scores.standardDeviation() > 20 && pValue < 70 -> QuestionDifficultyTag.CONFUSING
            else -> QuestionDifficultyTag.NORMAL
        }
    }

    private fun List<Double>.standardDeviation(): Double {
        if (isEmpty()) return 0.0
        val mean = average()
        val variance = map { value -> (value - mean) * (value - mean) }.average()
        return sqrt(variance)
    }

    private fun List<Assignment>.filterByClassroom(classroomId: String?): List<Assignment> =
        classroomId?.let { id -> filter { it.classroomId == id } } ?: this

    private fun classroomOptions(classrooms: List<Classroom>, selected: String?): List<ReportClassroomOption> {
        val active = classrooms.filterNot { it.isArchived }
        val options = mutableListOf(ReportClassroomOption(null, "All classrooms"))
        options += active.map { ReportClassroomOption(it.id, it.name.ifBlank { "Classroom" }) }
        val selectedExists = options.any { it.id == selected }
        if (!selectedExists && selected != null) {
            options += ReportClassroomOption(selected, "Selected classroom")
        }
        return options
    }

    private fun classroomDisplayName(assignments: List<Assignment>, classrooms: List<Classroom>, selected: String?): String {
        val activeClassrooms = classrooms.filterNot { it.isArchived }
        val relevantIds = assignments.map { it.classroomId }.distinct().toSet()
        val relevantClassrooms = activeClassrooms.filter { it.id in relevantIds }
        return when {
            selected != null -> activeClassrooms.firstOrNull { it.id == selected }?.name?.ifBlank { "Classroom" }
                ?: "Classroom"
            relevantClassrooms.size == 1 -> relevantClassrooms.first().name.ifBlank { "Classroom" }
            relevantClassrooms.isEmpty() && activeClassrooms.size == 1 -> activeClassrooms.first().name.ifBlank { "Classroom" }
            else -> "All classrooms"
        }
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

    private fun defaultReportsState(
        classroomName: String = "All classrooms",
        selectedClassroomId: String? = null,
        classroomOptions: List<ReportClassroomOption> = listOf(ReportClassroomOption(null, "All classrooms"))
    ) = ReportsUiState(
        isLoading = false,
        classroomName = classroomName,
        selectedClassroomId = selectedClassroomId,
        classroomOptions = classroomOptions,
        lastUpdated = null,
        lastUpdatedLabel = "Not updated yet"
    )
}
