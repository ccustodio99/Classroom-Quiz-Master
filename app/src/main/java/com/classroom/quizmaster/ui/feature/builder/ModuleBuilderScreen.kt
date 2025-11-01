package com.classroom.quizmaster.ui.feature.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.AdaptiveWrapRow
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.InfoPill
import com.classroom.quizmaster.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleBuilderScreen(
    viewModel: ModuleBuilderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val objectiveCount = remember(state.objectives) {
        state.objectives.split(',').map { it.trim() }.filter { it.isNotEmpty() }.size
    }
    val slideCount = remember(state.slides) {
        state.slides.lineSequence().map { it.trim() }.count { it.isNotEmpty() }
    }

    GenZScaffold(
        title = "Design module flow",
        subtitle = "Clarity • Consistency • Control for every learner",
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SectionCard(
                    title = "Module identity",
                    subtitle = "Start with the essentials so reports stay meaningful",
                    trailingContent = {
                        InfoPill(text = "$objectiveCount objectives")
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = state.topic,
                            onValueChange = viewModel::onTopicChanged,
                            label = { Text("Paksa / Topic") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.objectives,
                            onValueChange = viewModel::onObjectivesChanged,
                            label = { Text("Learning Objectives (comma separated)") },
                            supportingText = { Text("Tip: align with curriculum codes for sharper analytics") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                SectionCard(
                    title = "Lesson slides",
                    subtitle = "Keep a steady storytelling arc",
                    caption = "Each line becomes a card inside the live lesson. Use emojis or call-to-actions for energy.",
                    trailingContent = {
                        InfoPill(text = "$slideCount slides", backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.tertiary)
                    }
                ) {
                    OutlinedTextField(
                        value = state.slides,
                        onValueChange = viewModel::onSlidesChanged,
                        label = { Text("Slide content") },
                        supportingText = { Text("Preview: ${if (slideCount > 0) "${slideCount} cards queued" else "Add at least one slide"}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp)
                    )
                }
            }
            item {
                SectionCard(
                    title = "Question formats",
                    subtitle = "Blend quick checks with opinion pulses",
                    caption = "Rotate formats to keep energy high while staying aligned to objectives."
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "To test knowledge",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BulletPoint("Quiz — classic multiple choice with one or more correct answers.")
                            BulletPoint("True/False — fast binary checks to warm up the class.")
                            BulletPoint("Type Answer — short-form responses (≤20 characters) for precise recall.")
                            BulletPoint("Puzzle — drag blocks into the right order to sequence steps or sort ideas.")
                            BulletPoint("Slider — ask for a number on a scale, perfect for timelines or estimates.")
                        }
                        Text(
                            text = "To gather opinions (no points)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BulletPoint("Poll — capture sentiment with quick multiple choice taps.")
                            BulletPoint("Word Cloud — surface keywords that resonate with the class.")
                            BulletPoint("Open-Ended — let students share longer thoughts or reflections.")
                            BulletPoint("Brainstorm — co-create ideas and let the group upvote favourites.")
                        }
                    }
                }
            }
            item {
                SectionCard(
                    title = "Assessment pacing",
                    subtitle = "Balance focus with momentum",
                    caption = "A 45-60 second cadence works well for Gen Z attention spans while keeping rigour.",
                    trailingContent = {
                        InfoPill(text = "${state.timePerItem.ifBlank { "60" }}s")
                    }
                ) {
                    OutlinedTextField(
                        value = state.timePerItem,
                        onValueChange = viewModel::onTimePerItemChanged,
                        label = { Text("Time per item (seconds)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (state.errors.isNotEmpty()) {
                item {
                    SectionCard(
                        title = "Needs attention",
                        subtitle = "Fix these to keep the flow smooth",
                        caption = null,
                        trailingContent = {
                            InfoPill(
                                text = "${state.errors.size} items",
                                backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.errors.forEach { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            state.message?.let { message ->
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "UI/UX guardrails",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AdaptiveWrapRow(
                        horizontalSpacing = 8.dp,
                        verticalSpacing = 8.dp
                    ) {
                        listOf(
                            "Clarity",
                            "Consistency",
                            "Feedback",
                            "Efficiency",
                            "Visibility",
                            "Control",
                            "Delight"
                        ).forEach { principle ->
                            InfoPill(text = principle)
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { viewModel.save(onBack) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("I-save ang Module")
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
