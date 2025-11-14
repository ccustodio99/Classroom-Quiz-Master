package com.classroom.quizmaster.ui.preview

import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.ConnectionQuality
import com.classroom.quizmaster.ui.model.DistributionBar
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.QuizOverviewUi
import com.classroom.quizmaster.ui.model.SelectionOptionUi
import com.classroom.quizmaster.ui.model.ReportRowUi
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import com.classroom.quizmaster.ui.state.QuizRepositoryUi
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.ui.student.end.StudentEndUiState
import com.classroom.quizmaster.ui.student.entry.EntryTab
import com.classroom.quizmaster.ui.student.entry.LanHostUi
import com.classroom.quizmaster.ui.student.entry.StudentEntryUiState
import com.classroom.quizmaster.ui.student.lobby.StudentLobbyUiState
import com.classroom.quizmaster.ui.student.play.StudentPlayUiState
import com.classroom.quizmaster.ui.student.play.SubmissionStatus
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsUiState
import com.classroom.quizmaster.ui.teacher.home.ACTION_ASSIGNMENTS
import com.classroom.quizmaster.ui.teacher.home.ACTION_CREATE_QUIZ
import com.classroom.quizmaster.ui.teacher.home.ACTION_LAUNCH_SESSION
import com.classroom.quizmaster.ui.teacher.home.ACTION_REPORTS
import com.classroom.quizmaster.ui.teacher.home.ClassroomOverviewUi
import com.classroom.quizmaster.ui.teacher.home.HomeActionCard
import com.classroom.quizmaster.ui.teacher.home.TeacherHomeUiState
import com.classroom.quizmaster.ui.teacher.host.HostLiveUiState
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyUiState
import com.classroom.quizmaster.ui.teacher.quiz_editor.QuizEditorUiState
import com.classroom.quizmaster.ui.teacher.reports.ReportsUiState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private val sampleAvatars = listOf(
    AvatarOption("nova", "Nova", listOf(), "spark"),
    AvatarOption("bolt", "Bolt", listOf(), "atom"),
    AvatarOption("sage", "Sage", listOf(), "compass")
)

@Singleton
class FakeQuizRepository @Inject constructor() : QuizRepositoryUi {

    private val homeState = MutableStateFlow(
        TeacherHomeUiState(
            greeting = "Welcome back, Ms. Ramos",
            connectivityHeadline = "LAN connected | Cloud synced",
            connectivitySupporting = "Last sync 2 min ago",
            statusChips = listOf(
                StatusChipUi("lan", "LAN", StatusChipType.Lan),
                StatusChipUi("cloud", "Cloud", StatusChipType.Cloud),
                StatusChipUi("offline", "Offline-ready", StatusChipType.Offline)
            ),
            classrooms = listOf(
                ClassroomOverviewUi(
                    id = "1",
                    name = "Period 1 Algebra",
                    grade = "8",
                    topicCount = 4,
                    quizCount = 12
                ),
                ClassroomOverviewUi(
                    id = "2",
                    name = "STEM Club",
                    grade = null,
                    topicCount = 3,
                    quizCount = 6
                )
            ),
            actionCards = listOf(
                HomeActionCard(
                    id = ACTION_CREATE_QUIZ,
                    title = "Create a quiz",
                    description = "Build standards-aligned quizzes with templates.",
                    route = ACTION_CREATE_QUIZ,
                    ctaLabel = "Create quiz",
                    primary = true
                ),
                HomeActionCard(
                    id = ACTION_LAUNCH_SESSION,
                    title = "Launch live",
                    description = "Open a LAN lobby and start hosting.",
                    route = ACTION_LAUNCH_SESSION,
                    ctaLabel = "Launch lobby",
                    primary = true
                ),
                HomeActionCard(
                    id = ACTION_ASSIGNMENTS,
                    title = "Assignments",
                    description = "Schedule asynchronous practice.",
                    route = ACTION_ASSIGNMENTS,
                    ctaLabel = "Manage"
                ),
                HomeActionCard(
                    id = ACTION_REPORTS,
                    title = "Reports",
                    description = "Review insights and export data.",
                    route = ACTION_REPORTS,
                    ctaLabel = "View reports"
                )
            ),
            recentQuizzes = listOf(
                QuizOverviewUi(
                    "1",
                    "Fractions review",
                    "4",
                    "Math",
                    12,
                    78,
                    "2h ago",
                    false,
                    classroomName = "Period 1 Algebra",
                    topicName = "Fractions"
                ),
                QuizOverviewUi(
                    "2",
                    "Science trivia",
                    "5",
                    "Science",
                    15,
                    88,
                    "Yesterday",
                    true,
                    classroomName = "STEM Club",
                    topicName = "Space"
                )
            )
        )
    )

    private val editorState = MutableStateFlow(
        QuizEditorUiState(
            classroomId = "demo-classroom",
            topicId = "demo-topic",
            title = "Fractions review",
            grade = "4",
            subject = "Math",
            questions = listOf(
                QuestionDraftUi(
                    id = "q1",
                    stem = "What is 1/2 + 1/4?",
                    type = QuestionTypeUi.MultipleChoice,
                    answers = listOf(
                        AnswerOptionUi("a1", "A", "3/4", true),
                        AnswerOptionUi("a2", "B", "2/4", false)
                    ),
                    explanation = "Use like denominators."
                )
            ),
            classroomOptions = listOf(
                SelectionOptionUi("demo-classroom", "Period 1 Algebra", "Grade 4 • Math"),
                SelectionOptionUi("demo-classroom-2", "STEM Club", "Grade 5 • Science")
            ),
            topicsByClassroom = mapOf(
                "demo-classroom" to listOf(
                    SelectionOptionUi("demo-topic", "Fractions", "Number sense"),
                    SelectionOptionUi("demo-topic-2", "Decimals", "Place value")
                ),
                "demo-classroom-2" to listOf(
                    SelectionOptionUi("demo-topic-3", "Space", "Earth & Space"),
                    SelectionOptionUi("demo-topic-4", "Robotics", "STEM challenges")
                )
            )
        )
    )

    override val teacherHome: Flow<TeacherHomeUiState> = homeState.asStateFlow()

    override fun quizEditorState(quizId: String?): Flow<QuizEditorUiState> = editorState.asStateFlow()

    override suspend fun persistDraft(state: QuizEditorUiState) {
        editorState.value = state.copy(showSaveDialog = false)
    }
}

@Singleton
class FakeSessionRepository @Inject constructor() : SessionRepositoryUi {
    private val lobbyFlow = MutableStateFlow(
        LaunchLobbyUiState(
            joinCode = "R7FT",
            qrSubtitle = "09:12",
            discoveredPeers = 6,
            players = listOf(
                PlayerLobbyUi("1", "Kai", sampleAvatars[0], true, "Ready"),
                PlayerLobbyUi("2", "Lia", sampleAvatars[1], false, "Waiting")
            ),
            statusChips = listOf(
                StatusChipUi("lan", "LAN", StatusChipType.Lan),
                StatusChipUi("cloud", "Cloud", StatusChipType.Cloud)
            )
        )
    )

    private val hostFlow = MutableStateFlow(
        HostLiveUiState(
            questionIndex = 2,
            totalQuestions = 10,
            timerSeconds = 28,
            question = QuestionDraftUi(
                id = "hostQ",
                stem = "Which planet is red?",
                type = QuestionTypeUi.MultipleChoice,
                answers = listOf(
                    AnswerOptionUi("a", "A", "Mars", true),
                    AnswerOptionUi("b", "B", "Venus", false)
                ),
                explanation = "Mars dust is red."
            ),
            distribution = listOf(
                DistributionBar("A", 0.78f, true),
                DistributionBar("B", 0.12f),
                DistributionBar("C", 0.1f)
            ),
            leaderboard = listOf(
                LeaderboardRowUi(1, "Ava", 980, 12, sampleAvatars[0], true),
                LeaderboardRowUi(2, "Liam", 940, -5, sampleAvatars[1])
            )
        )
    )

    private val sampleLanHosts = listOf(
        LanHostUi(
            id = "room1",
            teacherName = "Ms. Lee",
            subject = "Science Quiz",
            players = 8,
            latencyMs = 12,
            joinCode = "SCILAN",
            quality = ConnectionQuality.Excellent,
            lastSeen = "moments ago"
        ),
        LanHostUi(
            id = "room2",
            teacherName = "Coach Diaz",
            subject = "History Warmup",
            players = 4,
            latencyMs = 18,
            joinCode = "HIST24",
            quality = ConnectionQuality.Good,
            lastSeen = "1 min ago"
        )
    )

    private val entryFlow = MutableStateFlow(
        StudentEntryUiState(
            tab = EntryTab.Lan,
            avatarOptions = sampleAvatars,
            lanHosts = sampleLanHosts,
            statusMessage = "LAN connected",
            lastSeenHosts = "moments ago",
            networkAvailable = true
        )
    )

    private val lobbyStudent = MutableStateFlow(
        StudentLobbyUiState(
            studentId = "demo-student",
            hostName = "Ms. Navarro",
            joinCode = sampleLanHosts.first().joinCode,
            joinStatus = "Waiting for host",
            players = lobbyFlow.value.players,
            lockedMessage = "Locked after Q1? No worries, you're in.",
            leaderboardPreview = hostFlow.value.leaderboard,
            connectionQuality = ConnectionQuality.Excellent
        )
    )

    private val playFlow = MutableStateFlow(
        StudentPlayUiState(
            question = hostFlow.value.question,
            timerSeconds = 24,
            progress = 0.6f,
            reveal = false,
            leaderboard = hostFlow.value.leaderboard,
            distribution = hostFlow.value.distribution,
            streak = 2,
            totalScore = 840,
            latencyMs = 18,
            connectionQuality = ConnectionQuality.Good,
            submissionMessage = "Tap an answer to submit"
        )
    )

    private val endFlow = MutableStateFlow(
        StudentEndUiState(
            stars = 3,
            rank = 4,
            team = "Galaxy Owls",
            badges = listOf("Fast thinker", "Team captain"),
            totalScore = 1320,
            improvement = 75,
            leaderboard = hostFlow.value.leaderboard,
            summary = "You outscored last game by 75 points!"
        )
    )

    override val launchLobby: Flow<LaunchLobbyUiState> = lobbyFlow.asStateFlow()
    override val hostState: Flow<HostLiveUiState> = hostFlow.asStateFlow()
    override val studentEntry: Flow<StudentEntryUiState> = entryFlow.asStateFlow()
    override val studentLobby: Flow<StudentLobbyUiState> = lobbyStudent.asStateFlow()
    override val studentPlay: Flow<StudentPlayUiState> = playFlow.asStateFlow()
    override val studentEnd: Flow<StudentEndUiState> = endFlow.asStateFlow()

    override suspend fun updateLeaderboardHidden(hidden: Boolean) {
        lobbyFlow.value = lobbyFlow.value.copy(hideLeaderboard = hidden)
        hostFlow.value = hostFlow.value.copy(showLeaderboard = !hidden)
    }

    override suspend fun updateLockAfterFirst(lock: Boolean) {
        lobbyFlow.value = lobbyFlow.value.copy(lockAfterFirst = lock)
        lobbyStudent.value = lobbyStudent.value.copy(
            lockedMessage = if (lock) "Host will lock joins after Q1" else null
        )
    }

    override suspend fun updateMuteSfx(muted: Boolean) {
        hostFlow.value = hostFlow.value.copy(muteSfx = muted)
    }

    override suspend fun startSession() {
        playFlow.value = playFlow.value.copy(
            submissionStatus = SubmissionStatus.Idle,
            submissionMessage = "Game starting"
        )
    }

    override suspend fun endSession() {}
    override suspend fun revealAnswer() {
        hostFlow.value = hostFlow.value.copy(isRevealed = true)
        playFlow.value = playFlow.value.copy(reveal = true)
    }

    override suspend fun nextQuestion() {
        hostFlow.value = hostFlow.value.copy(
            questionIndex = hostFlow.value.questionIndex + 1,
            isRevealed = false
        )
        playFlow.value = playFlow.value.copy(
            question = hostFlow.value.question,
            reveal = false,
            progress = 0.4f,
            submissionStatus = SubmissionStatus.Idle,
            submissionMessage = "Next question"
        )
    }

    override suspend fun kickParticipant(uid: String) {
        lobbyFlow.value = lobbyFlow.value.copy(
            players = lobbyFlow.value.players.filterNot { it.id == uid }
        )
    }

    override suspend fun toggleReady(studentId: String) {
        val currentlyReady = lobbyStudent.value.ready
        lobbyStudent.value = lobbyStudent.value.copy(ready = !currentlyReady)
        lobbyFlow.value = lobbyFlow.value.copy(
            players = lobbyFlow.value.players.map {
                if (it.id == studentId) it.copy(ready = !currentlyReady) else it
            }
        )
    }

    override suspend fun refreshLanHosts() {
        entryFlow.value = entryFlow.value.copy(isDiscovering = true, statusMessage = "Scanning for hosts...")
        entryFlow.value = entryFlow.value.copy(
            isDiscovering = false,
            lanHosts = sampleLanHosts.mapIndexed { index, host ->
                host.copy(lastSeen = if (index == 0) "just now" else host.lastSeen)
            },
            lastSeenHosts = "just now",
            statusMessage = "Hosts refreshed"
        )
    }

    override suspend fun joinLanHost(hostId: String, nickname: String, avatarId: String?): Result<Unit> {
        val host = entryFlow.value.lanHosts.firstOrNull { it.id == hostId }
            ?: return Result.failure(IllegalArgumentException("Host not found"))
        entryFlow.value = entryFlow.value.copy(isJoining = true, errorMessage = null)
        lobbyStudent.value = lobbyStudent.value.copy(
            hostName = host.teacherName,
            joinStatus = "Joined ${host.subject}",
            joinCode = host.joinCode,
            connectionQuality = host.quality,
            ready = true
        )
        entryFlow.value = entryFlow.value.copy(
            isJoining = false,
            selectedHostId = hostId,
            statusMessage = "Joined ${host.teacherName}'s lobby"
        )
        playFlow.value = playFlow.value.copy(
            totalScore = 0,
            streak = 0,
            submissionStatus = SubmissionStatus.Idle,
            submissionMessage = "Waiting for first question"
        )
        return Result.success(Unit)
    }

    override suspend fun joinWithCode(joinCode: String, nickname: String, avatarId: String?): Result<Unit> {
        val host = entryFlow.value.lanHosts.firstOrNull { it.joinCode.equals(joinCode, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Join code not recognized"))
        return joinLanHost(host.id, nickname, avatarId)
    }

    override suspend fun submitStudentAnswer(answerIds: List<String>) {
        playFlow.value = playFlow.value.copy(
            submissionStatus = SubmissionStatus.Acknowledged,
            submissionMessage = "Answer received in ${playFlow.value.latencyMs}ms",
            totalScore = playFlow.value.totalScore + 120,
            streak = playFlow.value.streak + 1
        )
        playFlow.value = playFlow.value.copy(
            leaderboard = playFlow.value.leaderboard.mapIndexed { index, row ->
                if (index == 0) row.copy(score = row.score + 120, delta = 18, isYou = true) else row
            }
        )
    }

    override suspend fun clearStudentError() {
        entryFlow.value = entryFlow.value.copy(errorMessage = null)
    }
}

@Singleton
class FakeAssignmentRepository @Inject constructor() : AssignmentRepositoryUi {
    private val assignmentFlow = MutableStateFlow(
        AssignmentsUiState(
            pending = listOf(
                AssignmentCardUi("1", "Homework 1", "due in 2 days", 18, 24, "Open")
            )
        )
    )

    private val reportsFlow = MutableStateFlow(
        ReportsUiState(
            average = 82,
            median = 78,
            topTopics = listOf("Fractions", "Ecosystems", "Grammar"),
            questionRows = listOf(
                ReportRowUi("Q1 Fractions", 0.65f, "B", 0.24f),
                ReportRowUi("Q2 Planets", 0.85f, "C", 0.12f)
            ),
            lastUpdated = "2m ago"
        )
    )

    override val assignments: Flow<AssignmentsUiState> = assignmentFlow.asStateFlow()
    override val reports: Flow<ReportsUiState> = reportsFlow.asStateFlow()
}
