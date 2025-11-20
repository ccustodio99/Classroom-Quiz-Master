package com.classroom.quizmaster.ui.teacher.materials

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.domain.model.MaterialAttachmentType
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.DropdownField
import com.classroom.quizmaster.ui.materials.MaterialSummaryUi
import com.classroom.quizmaster.ui.model.SelectionOptionUi

@Composable
fun TeacherMaterialsRoute(
    onBack: () -> Unit,
    onCreateMaterial: () -> Unit,
    onMaterialSelected: (String) -> Unit,
    onEditMaterial: (String) -> Unit,
    viewModel: TeacherMaterialsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    TeacherMaterialsScreen(
        state = state,
        onBack = onBack,
        onCreateMaterial = onCreateMaterial,
        onMaterialSelected = onMaterialSelected,
        onEditMaterial = onEditMaterial,
        onSelectClassroom = viewModel::selectClassroom,
        onSelectTopic = viewModel::selectTopic,
        onToggleArchived = viewModel::toggleArchived,
        onShare = viewModel::shareCurrentClassroom,
        onArchiveMaterial = viewModel::archive
    )
}

@Composable
fun TeacherMaterialsScreen(
    state: TeacherMaterialsUiState,
    onBack: () -> Unit,
    onCreateMaterial: () -> Unit,
    onMaterialSelected: (String) -> Unit,
    onEditMaterial: (String) -> Unit,
    onSelectClassroom: (String?) -> Unit,
    onSelectTopic: (String?) -> Unit,
    onToggleArchived: (Boolean) -> Unit,
    onShare: () -> Unit,
    onArchiveMaterial: (String) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Learning materials") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare, enabled = state.isShareEnabled) {
                        Icon(imageVector = Icons.Outlined.SyncAlt, contentDescription = "Share to students")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMaterial) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Create material")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ClassroomFilter(
                options = state.classroomOptions,
                selectedClassroomId = state.selectedClassroomId,
                onSelectClassroom = onSelectClassroom,
                topicOptions = state.topicOptions,
                selectedTopicId = state.selectedTopicId,
                onSelectTopic = onSelectTopic,
                showArchived = state.showArchived,
                onToggleArchived = onToggleArchived
            )
            if (state.materials.isEmpty()) {
                EmptyState(
                    title = "Nothing here",
                    message = state.emptyMessage.ifBlank { "Start by creating a material for your class." }
                )
                OutlinedButton(onClick = onCreateMaterial, modifier = Modifier.fillMaxWidth()) {
                    Text("Create material")
                }
            } else {
                MaterialsList(
                    materials = state.materials,
                    onMaterialSelected = onMaterialSelected,
                    onEditMaterial = onEditMaterial,
                    onArchiveMaterial = onArchiveMaterial
                )
            }
        }
    }
}

@Composable
private fun ClassroomFilter(
    options: List<SelectionOptionUi>,
    selectedClassroomId: String?,
    onSelectClassroom: (String?) -> Unit,
    topicOptions: List<SelectionOptionUi>,
    selectedTopicId: String?,
    onSelectTopic: (String?) -> Unit,
    showArchived: Boolean,
    onToggleArchived: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Filter", style = MaterialTheme.typography.titleMedium)
        DropdownField(
            label = "Classroom",
            items = options,
            selectedItem = options.firstOrNull { option -> option.id == selectedClassroomId },
            onItemSelected = { option -> onSelectClassroom(option.id.takeIf { it.isNotBlank() }) },
            itemLabel = { option -> option.label }
        )
        DropdownField(
            label = "Topic",
            items = topicOptions,
            selectedItem = topicOptions.firstOrNull { option -> option.id == selectedTopicId },
            onItemSelected = { option -> onSelectTopic(option.id.takeIf { it.isNotBlank() }) },
            itemLabel = { option -> option.label }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show archived", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Include archived materials in the list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = showArchived, onCheckedChange = onToggleArchived)
        }
    }
}

@Composable
private fun MaterialsList(
    materials: List<MaterialSummaryUi>,
    onMaterialSelected: (String) -> Unit,
    onEditMaterial: (String) -> Unit,
    onArchiveMaterial: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(materials, key = { it.id }) { material ->
            MaterialRow(
                summary = material,
                onClick = { onMaterialSelected(material.id) },
                onEdit = { onEditMaterial(material.id) },
                onArchive = { onArchiveMaterial(material.id) }
            )
        }
    }
}

@Composable
private fun MaterialRow(
    summary: MaterialSummaryUi,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val menuExpanded = remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .then(Modifier.padding(bottom = 8.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = summary.classroomName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (summary.topicName.isNotBlank()) {
                        Text(
                            text = summary.topicName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { menuExpanded.value = true }) {
                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "Material actions")
                }
                DropdownMenu(expanded = menuExpanded.value, onDismissRequest = { menuExpanded.value = false }) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = {
                        menuExpanded.value = false
                        onClick()
                    })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = {
                        menuExpanded.value = false
                        onEdit()
                    })
                    DropdownMenuItem(text = { Text("Archive") }, onClick = {
                        menuExpanded.value = false
                        onArchive()
                    })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.description.takeIf { it.isNotBlank() } ?: "No description provided",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttachmentTypeChips(summary.attachmentTypes)
                Text(
                    text = summary.updatedRelative,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AttachmentTypeChips(types: List<MaterialAttachmentType>) {
    val distinctTypes = types.distinct()
    if (distinctTypes.isEmpty()) {
        Text(
            text = "No attachments",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            distinctTypes.forEach { type ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                ) {
                    Text(
                        text = type.name.lowercase().replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
