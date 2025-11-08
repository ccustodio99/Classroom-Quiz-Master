package com.classroom.quizmaster.ui.preview

import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.DistributionBar
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.QuizOverviewUi
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
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsUiState
import com.classroom.quizmaster.ui.teacher.home.ACTION_ASSIGNMENTS
import com.classroom.quizmaster.ui.teacher.home.ACTION_CREATE_QUIZ
import com.classroom.quizmaster.ui.teacher.home.ACTION_LAUNCH_SESSION
import com.classroom.quizmaster.ui.teacher.home.ACTION_REPORTS
import com.classroom.quizmaster.ui.teacher.home.HomeActionCard
import com.classroom.quizmaster.ui.teacher.home.QuickStat
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
            quickStats = listOf(
                QuickStat("Active classes", "5", "+1 this week", true),
                QuickStat("Avg score", "83%", "+4 since Mon", true)
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
                QuizOverviewUi("1", "Fractions review", "4", "Math", 12, 78, "2h ago", false),
                QuizOverviewUi("2", "Science trivia", "5", "Science", 15, 88, "Yesterday", true)
            )
        )
    )

    private val editorState = MutableStateFlow(
        QuizEditorUiState(
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

    private val entryFlow = MutableStateFlow(
        StudentEntryUiState(
            tab = EntryTab.Lan,
            nickname = "",
            avatarOptions = sampleAvatars,
            lanHosts = listOf(
                LanHostUi("room1", "Ms. Lee", "Science Quiz", 8, 12),
                LanHostUi("room2", "Coach Diaz", "History Warmup", 4, 18)
            ),
            statusMessage = "LAN connected"
        )
    )

    private val lobbyStudent = MutableStateFlow(
        StudentLobbyUiState(
            studentId = "demo-student",
            hostName = "Ms. Navarro",
            joinStatus = "Waiting for host",
            avatars = sampleAvatars,
            lockedMessage = "Locked after Q1? No worries, you're in."
        )
    )

    private val playFlow = MutableStateFlow(
        StudentPlayUiState(
            question = hostFlow.value.question,
            timerSeconds = 24,
            progress = 0.6f,
            reveal = false
        )
    )

    private val endFlow = MutableStateFlow(
        StudentEndUiState(
            stars = 3,
            rank = 4,
            team = "Galaxy Owls",
            badges = listOf("Fast thinker", "Team captain")
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
        playFlow.value = playFlow.value.copy(feedback = "Game starting")
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
    }

    override suspend fun kickParticipant(uid: String) {
        lobbyFlow.value = lobbyFlow.value.copy(
            players = lobbyFlow.value.players.filterNot { it.id == uid }
        )
    }

    override suspend fun toggleReady(studentId: String) {
        lobbyStudent.value = lobbyStudent.value.copy(ready = !lobbyStudent.value.ready)
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
