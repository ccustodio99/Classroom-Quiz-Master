package com.classroom.quizmaster.ui.materials.editor

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.domain.model.MaterialAttachmentType
import com.classroom.quizmaster.ui.components.DropdownField
import com.classroom.quizmaster.ui.components.SimpleTopBar
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SecondaryButton

@Composable
fun MaterialEditorRoute(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: MaterialEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MaterialEditorEvent.Saved -> {
                    Toast.makeText(context, "Material saved", Toast.LENGTH_SHORT).show()
                    onSaved(event.materialId)
                }
                is MaterialEditorEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    MaterialEditorScreen(
        state = state,
        onBack = onBack,
        onTitleChanged = viewModel::updateTitle,
        onDescriptionChanged = viewModel::updateDescription,
        onBodyChanged = viewModel::updateBody,
        onClassroomChanged = viewModel::updateClassroom,
        onTopicChanged = viewModel::updateTopic,
        onAddAttachment = viewModel::addAttachment,
        onUpdateAttachment = viewModel::updateAttachment,
        onRemoveAttachment = viewModel::removeAttachment,
        onSave = viewModel::save
    )
}

@Composable
fun MaterialEditorScreen(
    state: MaterialEditorUiState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onBodyChanged: (String) -> Unit,
    onClassroomChanged: (String) -> Unit,
    onTopicChanged: (String) -> Unit,
    onAddAttachment: (MaterialAttachmentType) -> Unit,
    onUpdateAttachment: (String, (MaterialAttachmentDraft) -> MaterialAttachmentDraft) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSave: () -> Unit
) {
    val ready = state.classroomOptions.isNotEmpty()
    Scaffold(
        topBar = {
            SimpleTopBar(
                title = if (state.materialId.isNullOrBlank()) "New material" else "Edit material",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (ready) onAddAttachment(MaterialAttachmentType.TEXT) }) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Add note")
            }
        }
    ) { padding ->
        if (!ready) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create a classroom before adding materials.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp)
                )
                SecondaryButton(text = "Back", onClick = onBack)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = onTitleChanged,
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = onDescriptionChanged,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = onBodyChanged,
                        label = { Text("Body text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
                item {
                    DropdownField(
                        label = "Classroom",
                        items = state.classroomOptions,
                        selectedItem = state.classroomOptions.firstOrNull { option -> option.id == state.selectedClassroomId },
                        onItemSelected = { option -> onClassroomChanged(option.id) },
                        itemLabel = { option -> option.label }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DropdownField(
                        label = "Topic",
                        items = state.topicsByClassroom[state.selectedClassroomId].orEmpty(),
                        selectedItem = state.topicsByClassroom[state.selectedClassroomId]
                            ?.firstOrNull { option -> option.id == state.selectedTopicId },
                        onItemSelected = { option -> onTopicChanged(option.id) },
                        itemLabel = { option -> option.label }
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Attachments", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { onAddAttachment(MaterialAttachmentType.LINK) }) {
                            Text("Add link")
                        }
                    }
                }
                items(state.attachments, key = { it.id }) { draft ->
                    AttachmentEditorCard(
                        draft = draft,
                        onUpdate = { transform -> onUpdateAttachment(draft.id, transform) },
                        onRemove = { onRemoveAttachment(draft.id) }
                    )
                }
                item {
                    if (!state.errorMessage.isNullOrBlank()) {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    PrimaryButton(
                        text = if (state.isSaving) "Saving..." else "Save material",
                        onClick = onSave,
                        enabled = state.canSave && !state.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(text = "Cancel", onClick = onBack, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun AttachmentEditorCard(
    draft: MaterialAttachmentDraft,
    onUpdate: ((MaterialAttachmentDraft) -> MaterialAttachmentDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draft.displayName.ifBlank { draft.type.name.lowercase().replaceFirstChar { it.titlecase() } },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onRemove) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Remove attachment")
                }
            }
            AttachmentTypeDropdown(
                selected = draft.type,
                onSelected = { type -> onUpdate { it.copy(type = type) } }
            )
            OutlinedTextField(
                value = draft.displayName,
                onValueChange = { value -> onUpdate { it.copy(displayName = value) } },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth()
            )
            when (draft.type) {
                MaterialAttachmentType.TEXT -> {
                    OutlinedTextField(
                        value = draft.textContent,
                        onValueChange = { value -> onUpdate { it.copy(textContent = value) } },
                        label = { Text("Text content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
                MaterialAttachmentType.LINK, MaterialAttachmentType.VIDEO -> {
                    OutlinedTextField(
                        value = draft.uri,
                        onValueChange = { value -> onUpdate { it.copy(uri = value) } },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                MaterialAttachmentType.FILE -> {
                    OutlinedTextField(
                        value = draft.uri,
                        onValueChange = { value -> onUpdate { it.copy(uri = value) } },
                        label = { Text("File path or URI") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.mimeType,
                        onValueChange = { value -> onUpdate { it.copy(mimeType = value) } },
                        label = { Text("MIME type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentTypeDropdown(
    selected: MaterialAttachmentType,
    onSelected: (MaterialAttachmentType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val typeLabel: (MaterialAttachmentType) -> String = { type ->
        type.name.lowercase().replaceFirstChar { it.titlecase() }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = typeLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text("Attachment type") },
            trailingIcon = {
                val icon = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
                Icon(imageVector = icon, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { role = Role.DropdownList }
                .clickable { expanded = true }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MaterialAttachmentType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(typeLabel(type)) },
                    onClick = {
                        expanded = false
                        onSelected(type)
                    }
                )
            }
        }
    }
}
