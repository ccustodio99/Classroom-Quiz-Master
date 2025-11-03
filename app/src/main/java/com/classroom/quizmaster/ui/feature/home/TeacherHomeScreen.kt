package com.classroom.quizmaster.ui.feature.home

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.ui.feature.classroom.ClassroomEditorDialog
import com.classroom.quizmaster.ui.feature.classroom.ClassroomViewModel
import com.classroom.quizmaster.ui.feature.dashboard.DashboardViewModel
import com.classroom.quizmaster.ui.feature.dashboard.ModuleSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(
    classroomViewModel: ClassroomViewModel,
    dashboardViewModel: DashboardViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenClassDetail: (String) -> Unit,
    onOpenClassManager: () -> Unit,
    onCreateModule: (String?) -> Unit,
    onOpenModule: (String) -> Unit,
    onStartDelivery: (String) -> Unit,
    onOpenReports: (String) -> Unit,
    onJoinSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val classroomState by classroomViewModel.uiState.collectAsState()
    val moduleState by dashboardViewModel.uiState.collectAsState()

    LaunchedEffect(classroomState.message) {
        classroomState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            classroomViewModel.clearMessage()
        }
    }

    var selectedTab by remember { mutableStateOf(TeacherHomeTab.Classes) }

    val floatingAction: (@Composable () -> Unit)? = when (selectedTab) {
        TeacherHomeTab.Classes -> {
            classroomState.let {
                { AddClassFab(onClick = classroomViewModel::startCreate) }
            }
        }
        TeacherHomeTab.Calendar -> null
        TeacherHomeTab.ToDo -> null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.appBarTitle) },
                actions = {
                    if (selectedTab == TeacherHomeTab.Classes) {
                        IconButton(onClick = onOpenClassManager) {
                            Icon(Icons.Rounded.Class, contentDescription = "Manage classes")
                        }
                    }
                    IconButton(onClick = onJoinSession) {
                        Icon(Icons.Rounded.People, contentDescription = "Join live session")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                TeacherHomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        floatingActionButton = floatingAction
    ) { innerPadding ->
        when (selectedTab) {
            TeacherHomeTab.Classes -> ClassesTab(
                state = classroomState,
                onToggleArchived = classroomViewModel::toggleArchived,
                onEdit = classroomViewModel::startEdit,
                onArchiveToggle = classroomViewModel::setArchived,
                onOpenClassDetail = onOpenClassDetail,
                onCreateModule = { onCreateModule(null) },
                modifier = Modifier.padding(innerPadding)
            )
            TeacherHomeTab.Calendar -> CalendarTab(
                modules = moduleState.modules,
                modifier = Modifier.padding(innerPadding)
            )
            TeacherHomeTab.ToDo -> ToDoTab(
                modules = moduleState.modules,
                onOpenModule = onOpenModule,
                onStartDelivery = onStartDelivery,
                onOpenReports = onOpenReports,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    classroomState.editor?.let { editor ->
        ClassroomEditorDialog(
            state = editor,
            onDismiss = { classroomViewModel.dismissEditor() },
            onSave = { classroomViewModel.saveClassroom() },
            onUpdate = classroomViewModel::updateEditor
        )
    }
}

@Composable
private fun AddClassFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create class")
    }
}

@Composable
private fun ClassesTab(
    state: com.classroom.quizmaster.ui.feature.classroom.ClassroomUiState,
    onToggleArchived: (Boolean) -> Unit,
    onEdit: (ClassroomProfile) -> Unit,
    onArchiveToggle: (String, Boolean) -> Unit,
    onOpenClassDetail: (String) -> Unit,
    onCreateModule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !state.showArchived,
                onClick = { onToggleArchived(false) },
                label = { Text("Active") }
            )
            FilterChip(
                selected = state.showArchived,
                onClick = { onToggleArchived(true) },
                label = { Text("Archived") }
            )
        }

        if (state.classrooms.isEmpty()) {
            EmptyClassesState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.classrooms, key = { it.id }) { classroom ->
                    ClassCard(
                        profile = classroom,
                        onOpen = { onOpenClassDetail(classroom.id) },
                        onEdit = { onEdit(classroom) },
                        onCreateModule = onCreateModule,
                        onArchiveToggle = {
                            val archive = classroom.status != com.classroom.quizmaster.domain.model.ClassroomStatus.Archived
                            onArchiveToggle(classroom.id, archive)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassCard(
    profile: ClassroomProfile,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onCreateModule: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp,
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = profile.subject,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Class actions")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Open class") },
                        leadingIcon = { Icon(Icons.Rounded.Class, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onOpen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit details") },
                        leadingIcon = { Icon(Icons.Rounded.Assignment, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Create module") },
                        leadingIcon = { Icon(Icons.Rounded.Book, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onCreateModule()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (profile.status == com.classroom.quizmaster.domain.model.ClassroomStatus.Archived) "Restore" else "Archive") },
                        leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onArchiveToggle()
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(profile.gradeLevel) })
                if (profile.section.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text("Section ${profile.section}") })
                }
            }
            if (profile.description.isNotBlank()) {
                Text(profile.description, style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = onOpen) {
                Icon(Icons.Rounded.Book, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enter class")
            }
        }
    }
}

@Composable
private fun EmptyClassesState() {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No classes yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Just like Google Classroom, start by creating your first class so you can post modules and stream updates.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CalendarTab(modules: List<ModuleSummary>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (modules.isEmpty()) {
            item {
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Nothing scheduled", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Modules appear here when you assign due dates, mirroring Google Classroom's calendar view.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(modules, key = { it.id }) { module ->
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(module.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Objectives: ${module.objectives.joinToString()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                            Text("Set assignment due dates from the module detail screen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToDoTab(
    modules: List<ModuleSummary>,
    onOpenModule: (String) -> Unit,
    onStartDelivery: (String) -> Unit,
    onOpenReports: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Teacher to-do",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (modules.isEmpty()) {
            item {
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("All caught up!", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "When modules need delivery or grading, you'll see them listed here just like Google Classroom's to-do list.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(modules, key = { it.id }) { module ->
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(module.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Objectives: ${module.objectives.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(onClick = { onStartDelivery(module.id) }) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deliver")
                            }
                            OutlinedButton(onClick = { onOpenModule(module.id) }) {
                                Text("Details")
                            }
                            OutlinedButton(onClick = { onOpenReports(module.id) }) {
                                Text("Reports")
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class TeacherHomeTab(val label: String, val icon: ImageVector, val appBarTitle: String) {
    Classes("Classes", Icons.Rounded.Class, "Your classes"),
    Calendar("Calendar", Icons.Rounded.CalendarMonth, "Calendar"),
    ToDo("To-do", Icons.Rounded.Assignment, "To-do")
}
