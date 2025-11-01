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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
        subtitle = "Share the class code and watch responses roll in—no cloud required.",
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
        title = "Class code",
        subtitle = "Students join on the same network",
        caption = "Each code is local-only. Generate a fresh one anytime."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (sessionCode == null) {
                Text(
                    text = "Preparing your live session…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = sessionCode,
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
            }
        }
    }
}

@Composable
private fun ParticipantsSection(participants: List<ParticipantSummary>, total: Int) {
    SectionCard(
        title = "Participants",
        subtitle = "$total connected",
        caption = "Nicknames appear instantly—everything stays within the room."
    ) {
        if (participants.isEmpty()) {
            Text(
                text = "Waiting for students to join…",
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
        title = "Responses by question",
        subtitle = if (totalResponses == 0) "No responses yet" else "$totalResponses submissions logged",
        caption = "Track misconceptions without relying on the cloud."
    ) {
        if (responses.isEmpty()) {
            Text(
                text = "Responses will appear once answers are submitted.",
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
                text = "No responses yet",
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
