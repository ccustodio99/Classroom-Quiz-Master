package com.classroom.quizmaster.ui.feature.livesession

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.AdaptiveWrapRow
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.InfoPill
import com.classroom.quizmaster.ui.components.SectionCard
import com.classroom.quizmaster.ui.components.TopBarAction
import com.classroom.quizmaster.ui.feature.livesession.QuestionOption
import com.classroom.quizmaster.ui.strings.UiLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSessionScreen(
    viewModel: LiveSessionViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val actions = remember(state.sessionCode) {
        state.sessionCode?.let {
            listOf(
                TopBarAction(
                    icon = Icons.Rounded.Autorenew,
                    contentDescription = "Generate new code",
                    onClick = viewModel::regenerateSession
                )
            )
        } ?: emptyList()
    }

    GenZScaffold(
        title = state.moduleTopic ?: "Live session",
        subtitle = UiLabels.MODULE_FLOW_TAGLINE,
        onBack = onBack,
        actions = actions
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SessionCodeSection(
                sessionCode = state.sessionCode,
                totalParticipants = state.totalParticipants,
                totalResponses = state.totalResponses,
                onCopy = { code ->
                    clipboardManager.setText(AnnotatedString(code))
                    Toast.makeText(context, "Session code copied", Toast.LENGTH_SHORT).show()
                }
            )
            ActiveQuestionSection(
                questions = state.availableQuestions,
                activePrompt = state.activePrompt,
                activeObjective = state.activeObjective,
                onSelect = viewModel::setActiveQuestion
            )
            ParticipantsSection(participants = state.participants, total = state.totalParticipants)
            ResponsesSection(responses = state.responses, totalResponses = state.totalResponses)
        }
    }
}

@Composable
private fun SessionCodeSection(
    sessionCode: String?,
    totalParticipants: Int,
    totalResponses: Int,
    onCopy: (String) -> Unit
) {
    SectionCard(
        title = "Class code / Code ng klase",
        subtitle = "Students join on the same local network; walang internet? Walang problema.",
        caption = "Keep teacher and learners on one WiFi or LAN for smooth syncing."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (sessionCode == null) {
                Text(
                    text = "Preparing your live session..."
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val formattedCode = remember(sessionCode) {
                    sessionCode.chunked(3).joinToString(" ")
                }
                Text(
                    text = formattedCode,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                    InfoPill(text = "$totalParticipants joined")
                    InfoPill(text = "$totalResponses responses")
                }
                Button(
                    onClick = { onCopy(sessionCode) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null)
                    Text("Copy code", modifier = Modifier.padding(start = 8.dp))
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = "Pro tip: launch sessions while everyone is on the same classroom network for ultra-low latency.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveQuestionSection(
    questions: List<QuestionOption>,
    activePrompt: String?,
    activeObjective: String?,
    onSelect: (String?) -> Unit
) {
    SectionCard(
        title = "Active question / Aktibong tanong",
        subtitle = activePrompt ?: "No prompt pinned",
        caption = "Align ${UiLabels.PRE_TEST_PILL} and ${UiLabels.POST_TEST_PILL} prompts with the live item."
    ) {
        if (questions.isEmpty()) {
            Text(
                text = "Build ${UiLabels.PRE_TEST_PILL.lowercase()} and ${UiLabels.POST_TEST_PILL.lowercase()} items to enable live syncing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            var expanded by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = activePrompt ?: "Tap to choose",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pinned prompt") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No question pinned / Walang tanong") },
                            onClick = {
                                expanded = false
                                onSelect(null)
                            }
                        )
                        questions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(option.prompt)
                                        option.objective?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    onSelect(option.id)
                                }
                            )
                        }
                    }
                }
                if (!activeObjective.isNullOrBlank()) {
                    InfoPill(text = activeObjective)
                }
                TextButton(onClick = { onSelect(null) }) {
                    Text("Clear active question / Alisin ang pin")
                }
            }
        }
    }
}

@Composable
private fun ParticipantsSection(participants: List<ParticipantSummary>, total: Int) {
    SectionCard(
        title = "Participants / Mga kalahok",
        subtitle = "$total connected",
        caption = "Nicknames appear instantly; lahat ay naka-save nang lokal."
    ) {
        if (participants.isEmpty()) {
            Text(
                text = "Waiting for students to join / Naghihintay ng mga mag-aaral.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                participants.forEach { participant ->
                    ParticipantRow(participant)
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: ParticipantSummary) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(imageVector = Icons.Rounded.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = participant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${participant.answerCount} response${if (participant.answerCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResponsesSection(responses: List<ItemResponseSummary>, totalResponses: Int) {
    SectionCard(
        title = "Responses by question / Mga sagot bawat tanong",
        subtitle = if (totalResponses == 0) "No responses yet" else "$totalResponses submissions logged",
        caption = "Track misconceptions and ${UiLabels.MASTERY.lowercase()} without relying on the cloud."
    ) {
        if (responses.isEmpty()) {
            Text(
                text = "Responses will appear once answers are submitted / Lalabas ang datos kapag may sagot na.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                responses.forEach { summary ->
                    QuestionBreakdown(summary)
                }
            }
        }
    }
}

@Composable
private fun QuestionBreakdown(summary: ItemResponseSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = summary.prompt,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        summary.objective?.let { objective ->
            InfoPill(text = objective)
        }
        if (summary.answerCounts.isEmpty()) {
            Text(
                text = "No responses yet / Wala pang sagot",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                summary.answerCounts.entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                    .forEach { entry ->
                        val label = entry.key.ifBlank { "No answer" }
                        InfoPill(text = "$label (${entry.value})")
                    }
            }
        }
    }
}
