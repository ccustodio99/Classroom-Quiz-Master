package com.classroom.quizmaster.ui.materials.detail

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.domain.model.MaterialAttachment
import com.classroom.quizmaster.domain.model.MaterialAttachmentType
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SimpleTopBar
import com.classroom.quizmaster.ui.components.SecondaryButton

@Composable
fun MaterialDetailRoute(
    allowEditing: Boolean,
    allowShare: Boolean,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onArchived: () -> Unit,
    viewModel: MaterialDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effects.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(effect) {
        when (val current = effect) {
            is MaterialDetailEffect.Message -> {
                Toast.makeText(context, current.text, Toast.LENGTH_SHORT).show()
                viewModel.clearEffect()
            }
            MaterialDetailEffect.Archived -> {
                Toast.makeText(context, "Material archived", Toast.LENGTH_SHORT).show()
                viewModel.clearEffect()
                onArchived()
            }
            null -> Unit
        }
    }
    MaterialDetailScreen(
        state = state,
        allowEditing = allowEditing,
        allowShare = allowShare,
        onBack = onBack,
        onShare = viewModel::shareWithStudents,
        onEdit = onEdit,
        onArchive = viewModel::archiveMaterial
    )
}

@Composable
fun MaterialDetailScreen(
    state: MaterialDetailUiState,
    allowEditing: Boolean,
    allowShare: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEdit: (String) -> Unit,
    onArchive: () -> Unit
) {
    val material = state.material
    Scaffold(
        topBar = {
            SimpleTopBar(
                title = material?.title ?: "Material",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (allowShare && material != null) {
                        IconButton(onClick = onShare) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share over LAN")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (material == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Material not found", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
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
                    Text(text = material.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = material.classroomName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (material.topicName.isNotBlank()) {
                        Text(
                            text = material.topicName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = material.description.takeIf { it.isNotBlank() } ?: "No description provided",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Body", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = material.body.takeIf { it.isNotBlank() } ?: "No additional content",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                item {
                    Text("Attachments", style = MaterialTheme.typography.titleMedium)
                }
                if (material.attachments.isEmpty()) {
                    item {
                        Text(
                            text = "No attachments provided",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(material.attachments, key = { it.id }) { attachment ->
                        AttachmentCard(attachment = attachment)
                    }
                }
                if (allowEditing) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PrimaryButton(
                                text = "Edit",
                                modifier = Modifier.weight(1f),
                                onClick = { onEdit(material.id) }
                            )
                            SecondaryButton(
                                text = "Archive",
                                modifier = Modifier.weight(1f),
                                onClick = onArchive
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AttachmentCard(attachment: MaterialAttachment) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.displayName.ifBlank { attachment.type.name.lowercase().replaceFirstChar { it.titlecase() } },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = attachment.type.name.lowercase().replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (attachment.type != MaterialAttachmentType.TEXT && attachment.uri.isNotBlank()) {
                    IconButton(onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(attachment.uri))
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copy link")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            when (attachment.type) {
                MaterialAttachmentType.TEXT -> {
                    Text(
                        text = attachment.metadata["text"].orEmpty(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                MaterialAttachmentType.LINK, MaterialAttachmentType.VIDEO -> {
                    Text(
                        text = attachment.uri.ifBlank { "No link provided" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                MaterialAttachmentType.FILE -> {
                    Text(
                        text = attachment.uri.ifBlank { "File path not set" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!attachment.mimeType.isNullOrBlank()) {
                        Text(
                            text = attachment.mimeType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
