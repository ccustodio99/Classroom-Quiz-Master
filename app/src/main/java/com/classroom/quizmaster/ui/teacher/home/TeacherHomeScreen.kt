package com.classroom.quizmaster.ui.teacher.home

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.ConnectivityBanner
import com.classroom.quizmaster.ui.components.EmptyState
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
import com.classroom.quizmaster.ui.teacher.home.ACTION_REPORTS

@Composable
fun TeacherHomeRoute(
    onCreateClassroom: () -> Unit,
    onCreateQuiz: (String, String) -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit,
    onViewArchived: () -> Unit,
    onClassroomSelected: (String) -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.sampleSeedMessage) {
        val message = state.sampleSeedMessage
        if (!message.isNullOrBlank() && !state.isSeedingSamples && !state.isClearingSamples) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    TeacherHomeScreen(
        state = state,
        onCreateClassroom = onCreateClassroom,
        onCreateQuiz = onCreateQuiz,
        onAssignments = onAssignments,
        onReports = onReports,
        onViewArchived = onViewArchived,
        onSeedSampleData = viewModel::seedSampleData,
        onClearSampleData = viewModel::clearSampleData,
        onClassroomSelected = onClassroomSelected
    )
}

@Composable
fun TeacherHomeScreen(
    state: TeacherHomeUiState,
    onCreateClassroom: () -> Unit,
    onCreateQuiz: (String, String) -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit,
    onViewArchived: () -> Unit,
    onSeedSampleData: () -> Unit,
    onClearSampleData: () -> Unit,
    onClassroomSelected: (String) -> Unit
) {
    val hasClassrooms = state.classrooms.isNotEmpty()
    val hasTopics = state.classrooms.any { it.topicCount > 0 }
    val defaultClassroomId = state.defaultClassroomId
    val defaultTopicId = state.defaultTopicId
    val canCreateQuiz = !defaultClassroomId.isNullOrBlank() && !defaultTopicId.isNullOrBlank()
    val createQuizAction = {
        if (canCreateQuiz) {
            onCreateQuiz(defaultClassroomId!!, defaultTopicId!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = state.greeting, style = MaterialTheme.typography.headlineLarge)
        if (state.teacherName.isNotBlank()) {
            Text(
                text = state.teacherName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        ClassroomsSection(
            classrooms = state.classrooms,
            onCreateClassroom = onCreateClassroom,
            onViewArchived = onViewArchived,
            onClassroomSelected = onClassroomSelected
        )
        ActionCards(
            actionCards = state.actionCards,
            hasTopics = hasTopics,
            canCreateQuiz = canCreateQuiz,
            onCreateQuiz = createQuizAction,
            onAssignments = onAssignments,
            onReports = onReports
        )
        RecentQuizzesSection(
            quizzes = state.recentQuizzes,
            hasTopics = hasTopics,
            canCreateQuiz = canCreateQuiz,
            onCreateQuiz = if (hasTopics) createQuizAction else onCreateClassroom,
            emptyMessage = state.emptyMessage
        )
        if (state.showSampleDataCard) {
            SampleDataCard(
                canSeed = state.canSeedSampleData,
                canClear = state.canClearSampleData,
                isSeeding = state.isSeedingSamples,
                isClearing = state.isClearingSamples,
                message = state.sampleSeedMessage,
                onSeed = onSeedSampleData,
                onClear = onClearSampleData
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ClassroomsSection(
    classrooms: List<ClassroomOverviewUi>,
    onCreateClassroom: () -> Unit,
    onViewArchived: () -> Unit,
    onClassroomSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Classrooms", style = MaterialTheme.typography.titleLarge)
        SecondaryButton(
            text = "View archived",
            onClick = onViewArchived,
            modifier = Modifier.fillMaxWidth()
        )
        if (classrooms.isEmpty()) {
            EmptyState(
                title = "No classrooms yet",
                message = "Create a classroom to start organizing topics and quizzes."
            )
            PrimaryButton(
                text = "Create classroom",
                onClick = onCreateClassroom,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            classrooms.forEach { classroom ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClassroomSelected(classroom.id) },
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = classroom.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val meta = buildString {
                            classroom.grade?.takeIf { it.isNotBlank() }?.let {
                                append("Grade $it")
                            }
                            if (isNotEmpty()) {
                                append(" - ")
                            }
                            append("${classroom.topicCount} topics")
                            append(" - ${classroom.quizCount} quizzes")
                        }
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleDataCard(
    canSeed: Boolean,
    canClear: Boolean,
    isSeeding: Boolean,
    isClearing: Boolean,
    message: String?,
    onSeed: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Need sample data?", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Add demo classrooms, topics, quizzes, and assignments for exploration. This runs only in debug builds and never overwrites existing work.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val statusMessage = message ?: if (canClear || isClearing) {
                "Sample data is available in your classrooms."
            } else {
                null
            }
            statusMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (canSeed || isSeeding) {
                PrimaryButton(
                    text = if (isSeeding) "Seeding..." else "Add sample data",
                    onClick = onSeed,
                    enabled = canSeed && !isSeeding
                )
            }
            if (canClear || isClearing) {
                SecondaryButton(
                    text = if (isClearing) "Removing..." else "Remove sample data",
                    onClick = onClear,
                    enabled = canClear && !isClearing
                )
            }
        }
    }
}



@Composable
private fun ActionCards(
    actionCards: List<HomeActionCard>,
    hasTopics: Boolean,
    canCreateQuiz: Boolean,
    onCreateQuiz: () -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit
) {
    Text(text = "Actions", style = MaterialTheme.typography.titleLarge)
    val cards = if (actionCards.isEmpty()) defaultActionCards else actionCards
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.forEach { card ->
            val baseAction = resolveAction(
                card = card,
                onCreateQuiz = onCreateQuiz,
                onAssignments = onAssignments,
                onReports = onReports
            )

            val allowed = when (card.id) {
                ACTION_CREATE_QUIZ -> hasTopics && canCreateQuiz
                ACTION_ASSIGNMENTS -> hasTopics
                ACTION_REPORTS -> true
                else -> true
            }

            val enabled = baseAction != null && allowed
            ActionCard(
                card = card,
                onClick = {
                    if (enabled) {
                        baseAction?.invoke()
                    }
                },
                enabled = enabled
            )
        }
    }
}

private fun resolveAction(
    card: HomeActionCard,
    onCreateQuiz: () -> Unit,
    onAssignments: () -> Unit,
    onReports: () -> Unit
): (() -> Unit)? {
    val primary = when (card.id) {
        ACTION_CREATE_QUIZ -> onCreateQuiz
        ACTION_ASSIGNMENTS -> onAssignments
        ACTION_REPORTS -> onReports
        else -> null
    }
    if (primary != null) return primary
    return when (card.route) {
        ACTION_CREATE_QUIZ -> onCreateQuiz
        ACTION_ASSIGNMENTS -> onAssignments
        ACTION_REPORTS -> onReports
        else -> null
    }
}

@Composable
private fun RecentQuizzesSection(
    quizzes: List<QuizOverviewUi>,
    hasTopics: Boolean,
    canCreateQuiz: Boolean,
    onCreateQuiz: () -> Unit,
    emptyMessage: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Recent quizzes", style = MaterialTheme.typography.titleLarge)
        if (quizzes.isEmpty()) {
            val message = when {
                !hasTopics ->
                    "Add a topic inside one of your classrooms to start building quizzes."
                else ->
                    emptyMessage?.takeIf { it.isNotBlank() }
                        ?: "No recent quizzes"
            }
            Text(message)

            val buttonLabel = if (hasTopics && canCreateQuiz) "Create quiz" else "Create classroom"
            PrimaryButton(
                text = buttonLabel,
                onClick = onCreateQuiz,
                enabled = if (hasTopics) canCreateQuiz else true
            )
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
                        val location = listOf(quiz.classroomName, quiz.topicName)
                            .filter { it.isNotBlank() }
                            .joinToString("  -  ")
                        if (location.isNotBlank()) {
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val subjectAndGrade = listOf(
                            quiz.subject.takeIf { it.isNotBlank() },
                            quiz.grade.takeIf { it.isNotBlank() }?.let { "Grade $it" }
                        ).filterNotNull().joinToString("  -  ")
                        if (subjectAndGrade.isNotBlank()) {
                            Text(subjectAndGrade)
                        }
                        Text(text = "${quiz.questionCount} questions - ${quiz.averageScore}% avg")
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
                teacherName = "Ms. Ramos",
                connectivityHeadline = "LAN connected | Cloud synced",
                connectivitySupporting = "Last sync 2 min ago",
                statusChips = listOf(
                    StatusChipUi("lan", "LAN", StatusChipType.Lan),
                    StatusChipUi("cloud", "Cloud", StatusChipType.Cloud)
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
                actionCards = defaultActionCards,
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
                ),
                emptyMessage = "Import quizzes or create a new one to see it here"
            ),
            onCreateClassroom = {},
            onCreateQuiz = { _: String, _: String -> },
            onAssignments = {},
            onReports = {},
            onViewArchived = {},
            onSeedSampleData = {},
            onClearSampleData = {},
            onClassroomSelected = {}
        )
    }
}
