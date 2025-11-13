package com.classroom.quizmaster.ui.teacher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.R
import com.classroom.quizmaster.ui.components.ConnectivityBanner
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.model.QuizOverviewUi
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import com.classroom.quizmaster.ui.teacher.home.ACTION_ASSIGNMENTS
import com.classroom.quizmaster.ui.teacher.home.ACTION_CREATE_QUIZ
import com.classroom.quizmaster.ui.teacher.home.ACTION_LAUNCH_SESSION
import com.classroom.quizmaster.ui.teacher.home.ACTION_REPORTS

@Composable
fun TeacherHomeRoute(
    onCreateQuiz: () -> Unit,
    onLaunchLive: () -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TeacherHomeScreen(
        state = state,
        onCreateQuiz = onCreateQuiz,
        onLaunchLive = onLaunchLive,
        onAssignments = onAssignments,
        onReports = onReports
    )
}

@Composable
fun TeacherHomeScreen(
    state: TeacherHomeUiState,
    onCreateQuiz: () -> Unit,
    onLaunchLive: () -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = state.greeting, style = MaterialTheme.typography.headlineLarge)
        if (
            state.connectivityHeadline.isNotBlank() ||
            state.connectivitySupporting.isNotBlank() ||
            state.statusChips.isNotEmpty()
        ) {
            ConnectivityBanner(
                headline = state.connectivityHeadline,
                supportingText = state.connectivitySupporting,
                statusChips = state.statusChips
            )
        }
        if (state.isOfflineDemo) {
            TagChip(text = "Offline demo activated")
        }
        QuickStatsSection(stats = state.quickStats)
        ActionCards(
            actionCards = state.actionCards,
            onCreateQuiz = onCreateQuiz,
            onLaunchLive = onLaunchLive,
            onAssignments = onAssignments,
            onReports = onReports
        )
        RecentQuizzesSection(
            quizzes = state.recentQuizzes,
            onLaunchLive = onLaunchLive,
            onCreateQuiz = onCreateQuiz,
            emptyMessage = state.emptyMessage
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun QuickStatsSection(stats: List<QuickStat>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Classroom pulse", style = MaterialTheme.typography.titleLarge)
        if (stats.isEmpty()) {
            QuickStartSteps()
        } else {
            stats.forEach { stat ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = stat.label, style = MaterialTheme.typography.bodySmall)
                            Text(text = stat.value, style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(
                            text = stat.trendLabel,
                            color = if (stat.positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStartSteps() {
    val steps = listOf(
        stringResource(id = R.string.teacher_home_quickstart_step_one),
        stringResource(id = R.string.teacher_home_quickstart_step_two),
        stringResource(id = R.string.teacher_home_quickstart_step_three)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, step ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCards(
    actionCards: List<HomeActionCard>,
    onCreateQuiz: () -> Unit,
    onLaunchLive: () -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit
) {
    Text(text = "Actions", style = MaterialTheme.typography.titleLarge)
    val cards = if (actionCards.isEmpty()) defaultActionCards else actionCards
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.forEach { card ->
            val action = resolveAction(card, onCreateQuiz, onLaunchLive, onAssignments, onReports)
            ActionCard(
                card = card,
                onClick = action ?: {},
                enabled = action != null
            )
        }
    }
}

private fun resolveAction(
    card: HomeActionCard,
    onCreateQuiz: () -> Unit,
    onLaunchLive: () -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit
): (() -> Unit)? {
    val primary = when (card.id) {
        ACTION_CREATE_QUIZ -> onCreateQuiz
        ACTION_LAUNCH_SESSION -> onLaunchLive
        ACTION_ASSIGNMENTS -> onAssignments
        ACTION_REPORTS -> onReports
        else -> null
    }
    if (primary != null) return primary
    return when (card.route) {
        ACTION_CREATE_QUIZ -> onCreateQuiz
        ACTION_LAUNCH_SESSION -> onLaunchLive
        ACTION_ASSIGNMENTS -> onAssignments
        ACTION_REPORTS -> onReports
        else -> null
    }
}

@Composable
private fun RecentQuizzesSection(
    quizzes: List<QuizOverviewUi>,
    onLaunchLive: () -> Unit,
    onCreateQuiz: () -> Unit,
    emptyMessage: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Recent quizzes", style = MaterialTheme.typography.titleLarge)
        if (quizzes.isEmpty()) {
            Text(emptyMessage?.takeIf { it.isNotBlank() } ?: "Create your first quiz to see it here.")
            PrimaryButton(text = "Create quiz", onClick = onCreateQuiz)
        } else {
            quizzes.forEach { quiz ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = quiz.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = "${quiz.subject} - Grade ${quiz.grade}")
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${quiz.questionCount} questions - ${quiz.averageScore}% avg")
                            TextButton(onClick = onLaunchLive) {
                                Text("Launch")
                            }
                        }
                        Text(
                            text = quiz.updatedAgo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    card: HomeActionCard,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (card.primary) 4.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = iconForAction(card.id),
                    contentDescription = null
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = card.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = card.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val buttonModifier = Modifier.fillMaxWidth()
            if (card.primary) {
                PrimaryButton(
                    text = card.ctaLabel,
                    onClick = onClick,
                    modifier = buttonModifier,
                    enabled = enabled
                )
            } else {
                SecondaryButton(
                    text = card.ctaLabel,
                    onClick = onClick,
                    modifier = buttonModifier,
                    enabled = enabled
                )
            }
        }
    }
}

private fun iconForAction(actionId: String): ImageVector = when (actionId) {
    ACTION_CREATE_QUIZ -> Icons.Outlined.Quiz
    ACTION_LAUNCH_SESSION -> Icons.Outlined.FlashOn
    ACTION_ASSIGNMENTS -> Icons.Outlined.School
    ACTION_REPORTS -> Icons.Outlined.Timeline
    else -> Icons.Outlined.ArrowForward
}

private val defaultActionCards = listOf(
    HomeActionCard(
        id = ACTION_CREATE_QUIZ,
        title = "Create a quiz",
        description = "Build standards-aligned quizzes with question templates.",
        route = ACTION_CREATE_QUIZ,
        ctaLabel = "Create quiz",
        primary = true
    ),
    HomeActionCard(
        id = ACTION_LAUNCH_SESSION,
        title = "Launch a live session",
        description = "Open a LAN lobby and start playing instantly with your class.",
        route = ACTION_LAUNCH_SESSION,
        ctaLabel = "Launch lobby",
        primary = true
    ),
    HomeActionCard(
        id = ACTION_ASSIGNMENTS,
        title = "Manage assignments",
        description = "Schedule asynchronous practice with automatic grading.",
        route = ACTION_ASSIGNMENTS,
        ctaLabel = "Open assignments"
    ),
    HomeActionCard(
        id = ACTION_REPORTS,
        title = "Review reports",
        description = "Track mastery by standard and monitor growth over time.",
        route = ACTION_REPORTS,
        ctaLabel = "View reports"
    )
)

@QuizPreviews
@Composable
private fun TeacherHomePreview() {
    QuizMasterTheme {
        TeacherHomeScreen(
            state = TeacherHomeUiState(
                greeting = "Welcome back, Ms. Ramos",
                connectivityHeadline = "LAN connected | Cloud synced",
                connectivitySupporting = "Last sync 2 min ago",
                statusChips = listOf(
                    StatusChipUi("lan", "LAN", StatusChipType.Lan),
                    StatusChipUi("cloud", "Cloud", StatusChipType.Cloud)
                ),
                quickStats = listOf(
                    QuickStat("Active classes", "5", "+1 this week", true),
                    QuickStat("Avg score", "83%", "+4 since Mon", true)
                ),
                actionCards = defaultActionCards,
                recentQuizzes = listOf(
                    QuizOverviewUi("1", "Fractions review", "4", "Math", 12, 78, "2h ago", false),
                    QuizOverviewUi("2", "Science trivia", "5", "Science", 15, 88, "Yesterday", true)
                ),
                emptyMessage = "Import quizzes or create a new one to see it here"
            ),
            onCreateQuiz = {},
            onLaunchLive = {},
            onAssignments = {},
            onReports = {}
        )
    }
}
