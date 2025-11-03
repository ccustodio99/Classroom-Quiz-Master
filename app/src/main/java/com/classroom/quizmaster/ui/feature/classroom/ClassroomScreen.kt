
package com.classroom.quizmaster.ui.feature.classroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.domain.model.ClassroomStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen(
    viewModel: ClassroomViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onManageModules: (ClassroomProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classrooms") },
                navigationIcon = { OutlinedButton(onClick = onBack) { Text("Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.startCreate() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.showArchived,
                    onClick = { viewModel.toggleArchived(false) },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = state.showArchived,
                    onClick = { viewModel.toggleArchived(true) },
                    label = { Text("Archived") }
                )
            }
            if (state.classrooms.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Wala pang classroom.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.classrooms) { classroom ->
                        ClassroomRow(
                            profile = classroom,
                            onEdit = { viewModel.startEdit(classroom) },
                            onArchiveToggle = {
                                val archive = classroom.status != ClassroomStatus.Archived
                                viewModel.setArchived(classroom.id, archive)
                            },
                            onManageModules = { onManageModules(classroom) }
                        )
                    }
                }
            }
        }
    }

    state.editor?.let { editor ->
        ClassroomEditorDialog(
            state = editor,
            onDismiss = { viewModel.dismissEditor() },
            onSave = { viewModel.saveClassroom() },
            onUpdate = viewModel::updateEditor
        )
    }
}

@Composable
private fun ClassroomRow(
    profile: ClassroomProfile,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onManageModules: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(text = profile.subject, style = MaterialTheme.typography.bodyMedium)
        if (profile.section.isNotBlank()) {
            Text(text = "Section ${profile.section}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onManageModules) {
                Text("Modules")
            }
            OutlinedButton(onClick = onEdit) {
                Text("Edit")
            }
            TextButton(onClick = onArchiveToggle) {
                Text(if (profile.status == ClassroomStatus.Archived) "Restore" else "Archive")
            }
        }
    }
}
