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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.ConnectivityBanner
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.model.QuizOverviewUi
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

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
        ConnectivityBanner(
            headline = state.connectivityHeadline,
            supportingText = state.connectivitySupporting,
            statusChips = state.statusChips
        )
        if (state.isOfflineDemo) {
            TagChip(text = "Offline demo activated")
        }
        QuickStatsSection(stats = state.quickStats)
        ActionCards(
            state = state,
            onCreateQuiz = onCreateQuiz,
            onLaunchLive = onLaunchLive,
            onAssignments = onAssignments,
            onReports = onReports
        )
        RecentQuizzesSection(quizzes = state.recentQuizzes, onLaunchLive = onLaunchLive)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun QuickStatsSection(stats: List<QuickStat>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Classroom pulse", style = MaterialTheme.typography.titleLarge)
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = stat.label, style = MaterialTheme.typography.bodySmall)
                        Text(text = stat.value, style = MaterialTheme.typography.headlineSmall)
                    }
                    Text(
                        text = stat.trendLabel,
                        color = if (stat.positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCards(
    state: TeacherHomeUiState,
    onCreateQuiz: () -> Unit,
    onLaunchLive: () -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit
) {
    Text(text = "Actions", style = MaterialTheme.typography.titleLarge)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(text = "Create quiz", onClick = onCreateQuiz)
        PrimaryButton(text = "Launch live game", onClick = onLaunchLive)
        SecondaryButton(text = "Assignments", onClick = onAssignments)
        SecondaryButton(text = "Reports", onClick = onReports)
    }
}

@Composable
private fun RecentQuizzesSection(
    quizzes: List<QuizOverviewUi>,
    onLaunchLive: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Recent quizzes", style = MaterialTheme.typography.titleLarge)
        if (quizzes.isEmpty()) {
            Text("No quizzes yet. Create your first one!")
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
                    }
                }
            }
        }
    }
}

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
                recentQuizzes = listOf(
                    QuizOverviewUi("1", "Fractions review", "4", "Math", 12, 78, "2h ago", false),
                    QuizOverviewUi("2", "Science trivia", "5", "Science", 15, 88, "Yesterday", true)
                )
            ),
            onCreateQuiz = {},
            onLaunchLive = {},
            onAssignments = {},
            onReports = {}
        )
    }
}
