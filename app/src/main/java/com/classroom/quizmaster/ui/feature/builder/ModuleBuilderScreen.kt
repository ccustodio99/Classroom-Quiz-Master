package com.classroom.quizmaster.ui.feature.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.AdaptiveWrapRow
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.InfoPill
import com.classroom.quizmaster.ui.components.SectionCard
import com.classroom.quizmaster.domain.model.LearningMaterialType
import com.classroom.quizmaster.ui.feature.builder.InteractiveQuizDraft
import com.classroom.quizmaster.ui.feature.builder.LearningMaterialDraft
import com.classroom.quizmaster.ui.feature.builder.LessonTopicDraft
import kotlin.collections.buildList
import com.classroom.quizmaster.ui.feature.builder.MultipleChoiceDraft
import com.classroom.quizmaster.ui.feature.builder.PostTestItemDraft

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
    val topicSummary = remember(state.topics) {
        buildString {
            append(if (state.topics.size == 1) "1 topic" else "${state.topics.size} topics")
            val interactiveTotal = state.topics.sumOf { it.interactive.size }
            if (interactiveTotal > 0) {
                append(" • $interactiveTotal interactive")
            }
        }
    }

    val title = if (state.isEditing) "Update module flow" else "Design module flow"
    val subtitle = if (state.isEditing) {
        "Refresh objectives, slides, and reports before relaunching"
    } else {
        "Clarity • Consistency • Control for every learner"
    }
    GenZScaffold(
        title = title,
        subtitle = subtitle,
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
                    title = "Classroom setup",
                    subtitle = "Frame the space before crafting the flow",
                    caption = "Subject + classroom notes power reporting labels and live session codes.",
                    trailingContent = {
                        val summary = buildList {
                            if (state.subject.isNotBlank()) add(state.subject)
                            if (state.gradeLevel.isNotBlank()) add(state.gradeLevel)
                            if (state.section.isNotBlank()) add("Section ${state.section}")
                        }.joinToString(separator = " • ")
                        InfoPill(text = summary.ifBlank { "Set class profile" })
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = state.classroomName,
                            onValueChange = viewModel::onClassroomNameChanged,
                            label = { Text("Classroom or Subject name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.subject,
                            onValueChange = viewModel::onSubjectChanged,
                            label = { Text("Subject / Strand") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.gradeLevel,
                            onValueChange = viewModel::onGradeLevelChanged,
                            label = { Text("Grade level") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.section,
                            onValueChange = viewModel::onSectionChanged,
                            label = { Text("Section (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.classroomDescription,
                            onValueChange = viewModel::onClassroomDescriptionChanged,
                            label = { Text("Details / reminders") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                        )
                    }
                }
            }
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
                    title = "Topics, lessons, and assessments",
                    subtitle = "Capture objectives, materials, and quizzes per topic",
                    caption = "Build Kahoot-style interactions, attach learning materials, and set pre/post tests for every lesson.",
                    trailingContent = {
                        InfoPill(text = topicSummary.ifBlank { "Add topics" })
                    }
                ) {
                    LessonTopicsSection(
                        topics = state.topics,
                        viewModel = viewModel
                    )
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
                    title = "Interactive lesson pack",
                    subtitle = "Auto-generated blend of knowledge checks and opinion pulses",
                    caption = "Edit your objectives, slides, or pacing to instantly refresh the activities.",
                    trailingContent = {
                        val autoCount = state.interactivePreview.total
                        val topicCount = state.interactivePreview.topicInteractiveCount
                        val summary = if (topicCount > 0) {
                            "$autoCount auto • $topicCount topic"
                        } else {
                            "$autoCount auto"
                        }
                        InfoPill(text = summary)
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.interactivePreview.isEmpty()) {
                            Text(
                                text = "Magdagdag ng objectives at slides para makita ang interactive flow.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            InteractivePreviewGroup(
                                title = "To Test Knowledge",
                                count = state.interactivePreview.knowledgeCount,
                                entries = state.interactivePreview.knowledgeChecks
                            )
                            InteractivePreviewGroup(
                                title = "To Gather Opinions",
                                count = state.interactivePreview.opinionCount,
                                entries = state.interactivePreview.opinionPulse
                            )
                        }
                        Text(
                            text = "To Test Knowledge: Quiz, True/False, Type Answer, Puzzle, Slider. To Gather Opinions: Poll, Word Cloud, Open-Ended, Brainstorm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    val primaryLabel = if (state.isEditing) "I-update ang Module" else "I-save ang Module"
                    Button(
                        onClick = { viewModel.save(onBack) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(primaryLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractivePreviewGroup(
    title: String,
    count: Int,
    entries: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            InfoPill(text = "$count")
        }
        if (entries.isEmpty()) {
            Text(
                text = "No cards generated yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            entries.forEach { summary ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = summary,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonTopicsSection(
    topics: List<LessonTopicDraft>,
    viewModel: ModuleBuilderViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        topics.forEachIndexed { index, topic ->
            LessonTopicCard(
                index = index,
                topic = topic,
                allowRemove = topics.size > 1,
                viewModel = viewModel
            )
        }
        OutlinedButton(
            onClick = viewModel::addTopic,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add topic or lesson")
        }
    }
}

@Composable
private fun LessonTopicCard(
    index: Int,
    topic: LessonTopicDraft,
    allowRemove: Boolean,
    viewModel: ModuleBuilderViewModel
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Topic ${index + 1}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = { viewModel.removeTopic(topic.id) }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove topic")
                    }
                }
            }
            OutlinedTextField(
                value = topic.name,
                onValueChange = { viewModel.updateTopicName(topic.id, it) },
                label = { Text("Topic or lesson name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = topic.objectives,
                onValueChange = { viewModel.updateTopicObjectives(topic.id, it) },
                label = { Text("Learning objectives (comma or newline separated)") },
                supportingText = { Text("Example: LO1, LO2 or enter one objective per line") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = topic.details,
                onValueChange = { viewModel.updateTopicDetails(topic.id, it) },
                label = { Text("Details / lesson plan notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
            )
            Divider()
            LearningMaterialsSection(topicId = topic.id, materials = topic.materials, viewModel = viewModel)
            Divider()
            PreTestSection(topicId = topic.id, questions = topic.preTest, viewModel = viewModel)
            Divider()
            PostTestSection(topicId = topic.id, items = topic.postTest, viewModel = viewModel)
            Divider()
            InteractiveQuizSection(topicId = topic.id, quizzes = topic.interactive, viewModel = viewModel)
        }
    }
}

@Composable
private fun LearningMaterialsSection(
    topicId: String,
    materials: List<LearningMaterialDraft>,
    viewModel: ModuleBuilderViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Learning materials",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        materials.forEach { material ->
            LearningMaterialRow(
                topicId = topicId,
                material = material,
                allowRemove = materials.size > 1,
                viewModel = viewModel
            )
        }
        TextButton(onClick = { viewModel.addLearningMaterial(topicId) }) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add material")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearningMaterialRow(
    topicId: String,
    material: LearningMaterialDraft,
    allowRemove: Boolean,
    viewModel: ModuleBuilderViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Resource",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (allowRemove) {
                IconButton(onClick = { viewModel.removeLearningMaterial(topicId, material.id) }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove material")
                }
            }
        }
        OutlinedTextField(
            value = material.title,
            onValueChange = { title ->
                viewModel.updateLearningMaterial(topicId, material.id) { it.copy(title = title) }
            },
            label = { Text("Title / description") },
            modifier = Modifier.fillMaxWidth()
        )
        var expanded by remember(material.id) { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = material.type.displayName(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Material type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                LearningMaterialType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName()) },
                        onClick = {
                            viewModel.updateLearningMaterial(topicId, material.id) { it.copy(type = type) }
                            expanded = false
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = material.reference,
            onValueChange = { ref ->
                viewModel.updateLearningMaterial(topicId, material.id) { it.copy(reference = ref) }
            },
            label = { Text("Link, file name, or storage path") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Example: https://drive.link or AlgebraLesson.pptx") }
        )
    }
}

@Composable
private fun PreTestSection(
    topicId: String,
    questions: List<MultipleChoiceDraft>,
    viewModel: ModuleBuilderViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Pre-test (multiple choice)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        questions.forEachIndexed { index, question ->
            MultipleChoiceQuestionCard(
                title = "Question ${index + 1}",
                prompt = question.prompt,
                rationale = question.rationale,
                choices = question.choices,
                selectedAnswers = setOf(question.correctIndex),
                allowRemove = questions.size > 1,
                onPromptChange = { value ->
                    viewModel.updatePreTestQuestion(topicId, question.id) { it.copy(prompt = value) }
                },
                onChoiceChange = { optionIndex, value ->
                    viewModel.updatePreTestQuestion(topicId, question.id) {
                        val updated = it.choices.toMutableList().apply { set(optionIndex, value) }
                        it.copy(choices = updated)
                    }
                },
                onToggleAnswer = { answerIndex ->
                    viewModel.updatePreTestQuestion(topicId, question.id) { it.copy(correctIndex = answerIndex) }
                },
                onRationaleChange = { value ->
                    viewModel.updatePreTestQuestion(topicId, question.id) { it.copy(rationale = value) }
                },
                onRemove = { viewModel.removePreTestQuestion(topicId, question.id) }
            )
        }
        TextButton(onClick = { viewModel.addPreTestQuestion(topicId) }) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add pre-test question")
        }
    }
}

@Composable
private fun PostTestSection(
    topicId: String,
    items: List<PostTestItemDraft>,
    viewModel: ModuleBuilderViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Post-test (mix and match)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        items.forEachIndexed { index, item ->
            when (item) {
                is PostTestItemDraft.MultipleChoice -> {
                    MultipleChoiceQuestionCard(
                        title = "Multiple choice ${index + 1}",
                        prompt = item.prompt,
                        rationale = item.rationale,
                        choices = item.choices,
                        selectedAnswers = setOf(item.correctIndex),
                        allowRemove = items.size > 1,
                        onPromptChange = { value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.MultipleChoice).copy(prompt = value)
                            }
                        },
                        onChoiceChange = { optionIndex, value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                val source = it as PostTestItemDraft.MultipleChoice
                                val updated = source.choices.toMutableList().apply { set(optionIndex, value) }
                                source.copy(choices = updated)
                            }
                        },
                        onToggleAnswer = { answerIndex ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.MultipleChoice).copy(correctIndex = answerIndex)
                            }
                        },
                        onRationaleChange = { value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.MultipleChoice).copy(rationale = value)
                            }
                        },
                        onRemove = { viewModel.removePostTestItem(topicId, item.id) }
                    )
                }
                is PostTestItemDraft.TrueFalse -> {
                    TrueFalseItemCard(
                        title = "True or false ${index + 1}",
                        item = item,
                        allowRemove = items.size > 1,
                        onPromptChange = { value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.TrueFalse).copy(prompt = value)
                            }
                        },
                        onExplanationChange = { value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.TrueFalse).copy(explanation = value)
                            }
                        },
                        onAnswerChange = { answer ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.TrueFalse).copy(answer = answer)
                            }
                        },
                        onRemove = { viewModel.removePostTestItem(topicId, item.id) }
                    )
                }
                is PostTestItemDraft.Numeric -> {
                    NumericItemCard(
                        title = "Numeric ${index + 1}",
                        item = item,
                        allowRemove = items.size > 1,
                        onPromptChange = { value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.Numeric).copy(prompt = value)
                            }
                        },
                        onAnswerChange = { value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.Numeric).copy(answer = value)
                            }
                        },
                        onToleranceChange = { value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.Numeric).copy(tolerance = value)
                            }
                        },
                        onExplanationChange = { value ->
                            viewModel.updatePostTestItem(topicId, item.id) {
                                (it as PostTestItemDraft.Numeric).copy(explanation = value)
                            }
                        },
                        onRemove = { viewModel.removePostTestItem(topicId, item.id) }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { viewModel.addPostTestMultipleChoice(topicId) }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add multiple choice")
            }
            TextButton(onClick = { viewModel.addPostTestTrueFalse(topicId) }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add true/false")
            }
            TextButton(onClick = { viewModel.addPostTestNumeric(topicId) }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add numeric")
            }
        }
    }
}

@Composable
private fun InteractiveQuizSection(
    topicId: String,
    quizzes: List<InteractiveQuizDraft>,
    viewModel: ModuleBuilderViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Interactive quiz (Kahoot-style)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        quizzes.forEachIndexed { index, quiz ->
            InteractiveQuizCard(
                title = "Interactive ${index + 1}",
                quiz = quiz,
                allowRemove = quizzes.size > 1,
                onTitleChange = { value ->
                    viewModel.updateInteractiveQuiz(topicId, quiz.id) { it.copy(title = value) }
                },
                onPromptChange = { value ->
                    viewModel.updateInteractiveQuiz(topicId, quiz.id) { it.copy(prompt = value) }
                },
                onChoiceChange = { optionIndex, value ->
                    viewModel.updateInteractiveQuiz(topicId, quiz.id) {
                        val updated = it.options.toMutableList().apply { set(optionIndex, value) }
                        it.copy(options = updated)
                    }
                },
                onToggleAnswer = { answerIndex ->
                    viewModel.updateInteractiveQuiz(topicId, quiz.id) { current ->
                        val next = if (current.allowMultiple) {
                            val toggled = if (answerIndex in current.correctAnswers) {
                                current.correctAnswers - answerIndex
                            } else {
                                current.correctAnswers + answerIndex
                            }
                            if (toggled.isEmpty()) setOf(answerIndex) else toggled
                        } else {
                            setOf(answerIndex)
                        }
                        current.copy(correctAnswers = next)
                    }
                },
                onAllowMultipleChanged = { allowMultiple ->
                    viewModel.updateInteractiveQuiz(topicId, quiz.id) { current ->
                        val normalized = if (allowMultiple) {
                            current.correctAnswers
                        } else {
                            setOf(current.correctAnswers.firstOrNull() ?: 0)
                        }
                        current.copy(allowMultiple = allowMultiple, correctAnswers = normalized)
                    }
                },
                onRemove = { viewModel.removeInteractiveQuiz(topicId, quiz.id) }
            )
        }
        TextButton(onClick = { viewModel.addInteractiveQuiz(topicId) }) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add interactive quiz")
        }
    }
}

@Composable
private fun MultipleChoiceQuestionCard(
    title: String,
    prompt: String,
    rationale: String,
    choices: List<String>,
    selectedAnswers: Set<Int>,
    allowRemove: Boolean,
    onPromptChange: (String) -> Unit,
    onChoiceChange: (Int, String) -> Unit,
    onToggleAnswer: (Int) -> Unit,
    onRationaleChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove question")
                    }
                }
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            MultipleChoiceOptionsEditor(
                choices = choices,
                selectedIndices = selectedAnswers,
                onChoiceChange = onChoiceChange,
                onSelectionChange = onToggleAnswer
            )
            OutlinedTextField(
                value = rationale,
                onValueChange = onRationaleChange,
                label = { Text("Feedback / explanation") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
            )
        }
    }
}

@Composable
private fun TrueFalseItemCard(
    title: String,
    item: PostTestItemDraft.TrueFalse,
    allowRemove: Boolean,
    onPromptChange: (String) -> Unit,
    onExplanationChange: (String) -> Unit,
    onAnswerChange: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove item")
                    }
                }
            }
            OutlinedTextField(
                value = item.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = item.answer,
                    onClick = { onAnswerChange(true) },
                    label = { Text("True") }
                )
                FilterChip(
                    selected = !item.answer,
                    onClick = { onAnswerChange(false) },
                    label = { Text("False") }
                )
            }
            OutlinedTextField(
                value = item.explanation,
                onValueChange = onExplanationChange,
                label = { Text("Feedback / explanation") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
            )
        }
    }
}

@Composable
private fun NumericItemCard(
    title: String,
    item: PostTestItemDraft.Numeric,
    allowRemove: Boolean,
    onPromptChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onToleranceChange: (String) -> Unit,
    onExplanationChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove item")
                    }
                }
            }
            OutlinedTextField(
                value = item.prompt,
                onValueChange = onPromptChange,
                label = { Text("Problem statement") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = item.answer,
                onValueChange = onAnswerChange,
                label = { Text("Correct answer") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = item.tolerance,
                onValueChange = onToleranceChange,
                label = { Text("Accepted tolerance") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = item.explanation,
                onValueChange = onExplanationChange,
                label = { Text("Feedback / solution walkthrough") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
            )
        }
    }
}

@Composable
private fun InteractiveQuizCard(
    title: String,
    quiz: InteractiveQuizDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onChoiceChange: (Int, String) -> Unit,
    onToggleAnswer: (Int) -> Unit,
    onAllowMultipleChanged: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove interactive quiz")
                    }
                }
            }
            OutlinedTextField(
                value = quiz.title,
                onValueChange = onTitleChange,
                label = { Text("Quiz title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = quiz.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt or question") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (quiz.allowMultiple) "Multiple answers allowed" else "Single answer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = quiz.allowMultiple,
                    onCheckedChange = onAllowMultipleChanged
                )
            }
            MultipleChoiceOptionsEditor(
                choices = quiz.options,
                selectedIndices = quiz.correctAnswers,
                onChoiceChange = onChoiceChange,
                onSelectionChange = onToggleAnswer
            )
        }
    }
}

@Composable
private fun MultipleChoiceOptionsEditor(
    choices: List<String>,
    selectedIndices: Set<Int>,
    onChoiceChange: (Int, String) -> Unit,
    onSelectionChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEachIndexed { index, choice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val selected = index in selectedIndices
                FilterChip(
                    selected = selected,
                    onClick = { onSelectionChange(index) },
                    label = { Text(optionLabel(index)) }
                )
                OutlinedTextField(
                    value = choice,
                    onValueChange = { onChoiceChange(index, it) },
                    label = { Text("Choice ${optionLabel(index)}") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

private fun LearningMaterialType.displayName(): String = when (this) {
    LearningMaterialType.Document -> "Document / PDF"
    LearningMaterialType.Presentation -> "Presentation (PPT)"
    LearningMaterialType.Spreadsheet -> "Spreadsheet / Excel"
    LearningMaterialType.Media -> "Media (audio/video)"
    LearningMaterialType.Link -> "Link"
    LearningMaterialType.Other -> "Other"
}

private fun optionLabel(index: Int): String = ('A' + index).toString()

