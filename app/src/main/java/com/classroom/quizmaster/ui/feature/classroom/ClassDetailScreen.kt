package com.classroom.quizmaster.ui.feature.classroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.ui.feature.dashboard.ModuleSummary

@Composable
fun ClassDetailScreen(
    viewModel: ClassDetailViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCreateModule: (String) -> Unit,
    onOpenModule: (String) -> Unit,
    onStartDelivery: (String) -> Unit,
    onOpenReports: (String) -> Unit,
    onManageClass: (ClassroomProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    var selectedTab by remember { mutableStateOf(ClassDetailTab.Stream) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.classroom?.name ?: "Classroom",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.classroom?.let { classroom ->
                        IconButton(onClick = { onManageClass(classroom) }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Class settings")
                        }
                    }
                    IconButton(onClick = { selectedTab = ClassDetailTab.People }) {
                        Icon(Icons.Rounded.People, contentDescription = "View people")
                    }
                    IconButton(onClick = viewModel::refreshClassroom) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ClassDetailTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) },
                        icon = { Icon(tab.icon, contentDescription = null) }
                    )
                }
            }
            when (selectedTab) {
                ClassDetailTab.Stream -> StreamTab(state)
                ClassDetailTab.Classwork -> ClassworkTab(
                    state = state,
                    onCreateModule = onCreateModule,
                    onOpenModule = onOpenModule,
                    onStartDelivery = onStartDelivery,
                    onOpenReports = onOpenReports
                )
                ClassDetailTab.People -> PeopleTab(state)
                ClassDetailTab.Grades -> GradesTab(state, onOpenReports)
            }
        }
    }
}

@Composable
private fun StreamTab(state: ClassDetailUiState) {
    val classroom = state.classroom
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        classroom?.let { profile ->
            item {
                ClassHeroCard(profile)
            }
        }
        if (state.modules.isNotEmpty()) {
            item {
                Text(
                    text = "Recent posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(state.modules) { module ->
                StreamPostCard(module)
            }
        } else {
            item {
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Nothing posted yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Post a module from the Classwork tab to populate your stream, just like in Google Classroom.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassworkTab(
    state: ClassDetailUiState,
    onCreateModule: (String) -> Unit,
    onOpenModule: (String) -> Unit,
    onStartDelivery: (String) -> Unit,
    onOpenReports: (String) -> Unit
) {
    val classroom = state.classroom
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        classroom?.let { profile ->
            item {
                FilledTonalButton(
                    onClick = { onCreateModule(profile.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Assignment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create classwork module")
                }
            }
        }
        if (state.modules.isEmpty()) {
            item {
                EmptyClassworkState()
            }
        } else {
            items(state.modules, key = { it.id }) { module ->
                ClassworkCard(
                    module = module,
                    onOpenModule = onOpenModule,
                    onStartDelivery = onStartDelivery,
                    onOpenReports = onOpenReports
                )
            }
        }
    }
}

@Composable
private fun PeopleTab(state: ClassDetailUiState) {
    val classroom = state.classroom
    val roster = remember(classroom) {
        buildList {
            classroom?.let { profile ->
                add("Teacher: ${profile.ownerId ?: "You"}")
            }
            addAll(
                listOf(
                    "Student roster sync coming soon",
                    "Invite learners via session code"
                )
            )
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "People",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        items(roster) { entry ->
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                RowContent(
                    icon = Icons.Rounded.AccountCircle,
                    title = entry,
                    subtitle = "Tap manage class to sync accounts"
                )
            }
        }
    }
}

@Composable
private fun GradesTab(
    state: ClassDetailUiState,
    onOpenReports: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Grades overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (state.modules.isEmpty()) {
            item { EmptyGradesState() }
        } else {
            items(state.modules, key = { it.id }) { module ->
                GradeCard(module = module, onOpenReports = onOpenReports)
            }
        }
    }
}

@Composable
private fun ClassHeroCard(profile: ClassroomProfile) {
    Surface(
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = profile.subject,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (profile.section.isNotBlank()) {
                AssistChip(onClick = {}, label = { Text("Section ${profile.section}") })
            }
            if (profile.description.isNotBlank()) {
                Text(text = profile.description, style = MaterialTheme.typography.bodyMedium)
            }
            RowContent(
                icon = Icons.Rounded.CalendarToday,
                title = "All modules appear here",
                subtitle = "Mimics Google Classroom's Stream and Classwork separation"
            )
        }
    }
}

@Composable
private fun StreamPostCard(module: ModuleSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = module.topic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Learning goals: ${module.objectives.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ClassworkCard(
    module: ModuleSummary,
    onOpenModule: (String) -> Unit,
    onStartDelivery: (String) -> Unit,
    onOpenReports: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(module.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Objectives: ${module.objectives.joinToString()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(onClick = { onOpenModule(module.id) }) {
                    Text("Open")
                }
                OutlinedButton(onClick = { onStartDelivery(module.id) }) {
                    Text("Deliver")
                }
                OutlinedButton(onClick = { onOpenReports(module.id) }) {
                    Text("Reports")
                }
            }
        }
    }
}

@Composable
private fun EmptyClassworkState() {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No classwork yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Google Classroom starts with a simple empty state. Use the button above to create your first module and post it to the stream.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyGradesState() {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No grades yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Publish reports from modules to surface mastery insights here.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun GradeCard(module: ModuleSummary, onOpenReports: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(module.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "View analytics and PDF exports in one tap.",
                style = MaterialTheme.typography.bodyMedium
            )
            FilledTonalButton(onClick = { onOpenReports(module.id) }) {
                Text("Open reports")
            }
        }
    }
}

@Composable
private fun RowContent(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class ClassDetailTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Stream("Stream", Icons.Rounded.CalendarToday),
    Classwork("Classwork", Icons.Rounded.Assignment),
    People("People", Icons.Rounded.People),
    Grades("Grades", Icons.Rounded.Assignment)
}
