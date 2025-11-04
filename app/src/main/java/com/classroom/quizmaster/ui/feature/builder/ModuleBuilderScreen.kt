package com.classroom.quizmaster.ui.feature.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.AdaptiveWrapRow
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.InfoPill
import com.classroom.quizmaster.ui.components.SectionCard
import com.classroom.quizmaster.domain.model.LearningMaterialType
import com.classroom.quizmaster.ui.feature.builder.BrainstormInteractiveDraft
import com.classroom.quizmaster.ui.feature.builder.InteractiveDraft
import com.classroom.quizmaster.ui.feature.builder.InteractiveDraftType
import com.classroom.quizmaster.ui.feature.builder.LearningMaterialDraft
import com.classroom.quizmaster.ui.feature.builder.LessonTopicDraft
import com.classroom.quizmaster.ui.feature.builder.OpenEndedInteractiveDraft
import com.classroom.quizmaster.ui.feature.builder.PollInteractiveDraft
import com.classroom.quizmaster.ui.feature.builder.PuzzleInteractiveDraft
import com.classroom.quizmaster.ui.feature.builder.QuizInteractiveDraft
import kotlin.collections.buildList
import com.classroom.quizmaster.ui.feature.builder.MultipleChoiceDraft
import com.classroom.quizmaster.ui.feature.builder.PostTestItemDraft
import com.classroom.quizmaster.ui.feature.builder.SliderInteractiveDraft
import com.classroom.quizmaster.ui.feature.builder.TrueFalseInteractiveDraft
import com.classroom.quizmaster.ui.feature.builder.TypeAnswerInteractiveDraft
import com.classroom.quizmaster.ui.feature.builder.WordCloudInteractiveDraft
import com.classroom.quizmaster.ui.strings.UiLabels

@Composable
fun ModuleBuilderScreen(
    viewModel: ModuleBuilderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val objectiveCount = remember(state.objectives, state.topics) {
        val moduleObjectives = state.objectives
            .split(',', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val topicObjectives = state.topics.flatMap { topic ->
            topic.objectives
                .split(',', '\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
        (moduleObjectives + topicObjectives).toSet().size
    }
    val slideCount = remember(state.slides) {
        state.slides.lineSequence().map { it.trim() }.count { it.isNotEmpty() }
    }
    val resolvedModuleTopic = remember(state.topic, state.topics) {
        state.topic.trim().ifBlank {
            state.topics.firstOrNull { it.name.isNotBlank() }?.name?.trim().orEmpty()
        }
    }
    val topicSummary = remember(state.topics) {
        if (state.topics.isEmpty()) {
            ""
        } else {
            buildString {
                append(if (state.topics.size == 1) "1 topic" else "${state.topics.size} topics")
                val interactiveTotal = state.topics.sumOf { it.interactive.size }
                if (interactiveTotal > 0) {
                    append(" - $interactiveTotal interactive")
                }
            }
        }
    }

    val title = if (state.isEditing) "Update topic/module" else "Create topic/module"
    val subtitle = UiLabels.MODULE_FLOW_TAGLINE
    val identityValid = resolvedModuleTopic.isNotBlank() &&
        objectiveCount > 0 &&
        slideCount > 0 &&
        (state.timePerItem.toIntOrNull() ?: 0) > 0
    val hasIdentityDraft = state.moduleIdentitySaved ||
        state.topic.isNotBlank() ||
        state.objectives.isNotBlank() ||
        state.slides.isNotBlank() ||
        state.topics.isNotEmpty()
    val hasLessonDraft = state.slides.isNotBlank() ||
        state.topics.any { topic ->
            topic.name.isNotBlank() ||
                topic.objectives.isNotBlank() ||
                topic.details.isNotBlank() ||
                topic.materials.any { material ->
                    material.title.isNotBlank() || material.reference.isNotBlank()
                } ||
                topic.interactive.any { it.prompt.isNotBlank() }
        }
    val hasAssessmentDraft = state.topics.any { topic ->
        topic.preTest.isNotEmpty() || topic.postTest.isNotEmpty()
    } || state.interactivePreview.total > 0
    val lessonDraftPresent = hasLessonDraft
    val moduleStatus = when {
        identityValid -> StepStatus.Complete
        hasIdentityDraft -> StepStatus.Active
        else -> StepStatus.Pending
    }
    val lessonStatus = when {
        identityValid && lessonDraftPresent -> StepStatus.Complete
        lessonDraftPresent -> StepStatus.Active
        else -> StepStatus.Pending
    }
    val assessmentStatus = when {
        identityValid && hasAssessmentDraft -> StepStatus.Complete
        hasAssessmentDraft -> StepStatus.Active
        else -> StepStatus.Pending
    }
    val progressSteps = listOf(
        BuilderStep(
            title = "Module identity",
            description = "Topic, objectives, pacing, and slides",
            status = moduleStatus
        ),
        BuilderStep(
            title = "Lesson beats",
            description = "Lesson topics, materials, and interactives",
            status = lessonStatus
        ),
        BuilderStep(
            title = "Assessments",
            description = "Pre/Post items and mini checks",
            status = assessmentStatus
        )
    )
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
                BuilderProgressHeader(steps = progressSteps)
            }
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Classroom setup now lives in the Classrooms tab.",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Use the New Class flow to change subject, grade level, or section details. Those updates sync automatically when you open this builder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val classroomSummary = buildList {
                            state.subject.takeIf { it.isNotBlank() }?.let { add(it) }
                            state.gradeLevel.takeIf { it.isNotBlank() }?.let { add(it) }
                            state.section.takeIf { it.isNotBlank() }?.let { add("Section $it") }
                        }.joinToString(" \u2022 ")
                        if (classroomSummary.isNotBlank()) {
                            InfoPill(text = classroomSummary)
                        }
                    }
                }
            }
            item {
                SectionCard(
                    title = "Topics, lessons, and assessments",
                    subtitle = "Capture objectives, materials, and quizzes per topic",
                    caption = "Build Kahoot-style interactions, attach learning materials, and set pre/post tests for every lesson.",
                    trailingContent = {
                        val pillText = when {
                            topicSummary.isBlank() -> "Draft topics"
                            identityValid -> topicSummary
                            else -> "$topicSummary draft"
                        }
                        InfoPill(text = pillText)
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (!identityValid) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    text = "Draft your lessons now. Once the basics above are complete, saving the module locks analytics labels.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        LessonSlidesSubsection(
                            slideCount = slideCount,
                            slides = state.slides,
                            onSlidesChanged = viewModel::onSlidesChanged
                        )
                        InteractiveLessonPackSubsection(preview = state.interactivePreview)
                        AssessmentPacingSubsection(
                            timePerItem = state.timePerItem,
                            onTimePerItemChanged = viewModel::onTimePerItemChanged
                        )
                        LessonTopicsSection(
                            topics = state.topics,
                            viewModel = viewModel
                        )
                    }
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
                        enabled = identityValid && lessonDraftPresent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(primaryLabel)
                    }
                }
            }
        }
    }
}

private data class BuilderStep(
    val title: String,
    val description: String,
    val status: StepStatus
)

private enum class StepStatus { Complete, Active, Pending }

@Composable
private fun BuilderProgressHeader(steps: List<BuilderStep>) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Module flow progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Work through each step from top to bottom. Saving the module will lock in anything marked complete.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            steps.forEach { step ->
                BuilderStepRow(step)
            }
        }
    }
}

@Composable
private fun BuilderStepRow(step: BuilderStep) {
    val indicatorColor = when (step.status) {
        StepStatus.Complete -> MaterialTheme.colorScheme.primary
        StepStatus.Active -> MaterialTheme.colorScheme.tertiary
        StepStatus.Pending -> MaterialTheme.colorScheme.outline
    }
    val label = when (step.status) {
        StepStatus.Complete -> "Complete"
        StepStatus.Active -> "In progress"
        StepStatus.Pending -> "Pending"
    }
    val labelBackground = when (step.status) {
        StepStatus.Pending -> MaterialTheme.colorScheme.surfaceVariant
        else -> indicatorColor.copy(alpha = 0.18f)
    }
    val labelColor = when (step.status) {
        StepStatus.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> indicatorColor
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(14.dp),
            shape = CircleShape,
            color = indicatorColor
        ) {}
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = labelBackground,
            contentColor = labelColor,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
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
        FilledTonalButton(
            onClick = viewModel::addTopic,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add topic / lesson / assessments")
        }

        if (topics.isEmpty()) {
            Text(
                text = "Walang lesson beats pa. Magdagdag ng topic para maayos ang lesson flow at assessments.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            topics.forEachIndexed { index, topic ->
                LessonTopicCard(
                    index = index,
                    topic = topic,
                    allowRemove = topics.isNotEmpty(),
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun LessonSlidesSubsection(
    slideCount: Int,
    slides: String,
    onSlidesChanged: (String) -> Unit
) {
    TopicSubsection(
        title = "Lesson slides",
        subtitle = "Keep a steady storytelling arc",
        badge = {
            val text = when (slideCount) {
                0 -> "No slides"
                1 -> "1 slide"
                else -> "$slideCount slides"
            }
            InfoPill(
                text = text,
                backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                contentColor = MaterialTheme.colorScheme.tertiary
            )
        }
    ) {
        OutlinedTextField(
            value = slides,
            onValueChange = onSlidesChanged,
            label = { Text("Slide content") },
            supportingText = { Text("Preview: ${if (slideCount > 0) "$slideCount cards queued" else "Add at least one slide"}") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
        )
    }
}

@Composable
private fun InteractiveLessonPackSubsection(preview: InteractivePreviewSummary) {
    val autoCount = preview.total
    val topicCount = preview.topicInteractiveCount
    val summary = when {
        autoCount == 0 && topicCount == 0 -> "No auto activities"
        topicCount > 0 -> "$autoCount auto, $topicCount topic"
        else -> "$autoCount auto"
    }
    TopicSubsection(
        title = "Interactive lesson pack",
        subtitle = "Auto-generated blend of knowledge checks and opinion pulses",
        badge = { InfoPill(text = summary) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (preview.isEmpty()) {
                Text(
                    text = "Magdagdag ng objectives at slides para makita ang interactive flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InteractivePreviewGroup(
                    title = "To Test Knowledge",
                    count = preview.knowledgeCount,
                    entries = preview.knowledgeChecks
                )
                InteractivePreviewGroup(
                    title = "To Gather Opinions",
                    count = preview.opinionCount,
                    entries = preview.opinionPulse
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

@Composable
private fun AssessmentPacingSubsection(
    timePerItem: String,
    onTimePerItemChanged: (String) -> Unit
) {
    val pacing = timePerItem.ifBlank { "60" }
    TopicSubsection(
        title = "Assessment pacing",
        subtitle = "Balance focus with momentum",
        badge = { InfoPill(text = "${pacing}s") }
    ) {
        OutlinedTextField(
            value = timePerItem,
            onValueChange = onTimePerItemChanged,
            label = { Text("Time per item (seconds)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TopicSubsection(
    title: String,
    subtitle: String,
    badge: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                badge?.let {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        it()
                    }
                }
            }
            content()
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
            InteractiveActivitiesSection(
                topicId = topic.id,
                activities = topic.interactive,
                viewModel = viewModel
            )
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
        var textFieldSize by remember(material.id) { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current
        val dropdownWidth = if (textFieldSize.width > 0) {
            with(density) { textFieldSize.width.toDp() }
        } else {
            240.dp
        }
        Box {
            OutlinedTextField(
                value = material.type.displayName(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Material type") },
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        val icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
                        Icon(
                            imageVector = icon,
                            contentDescription = if (expanded) {
                                "Collapse material types"
                            } else {
                                "Expand material types"
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        textFieldSize = coordinates.size
                    }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(dropdownWidth)
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
        if (questions.isEmpty()) {
            Text(
                text = "Optional: magdagdag ng diagnostic question para sa paksang ito.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        questions.forEachIndexed { index, question ->
            MultipleChoiceQuestionCard(
                title = "Question ${index + 1}",
                prompt = question.prompt,
                rationale = question.rationale,
                choices = question.choices,
                selectedAnswers = setOf(question.correctIndex),
                allowRemove = true,
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
        if (items.isEmpty()) {
            Text(
                text = "Optional: magdagdag ng follow-up assessment pagkatapos ng topic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items.forEachIndexed { index, item ->
            when (item) {
                is PostTestItemDraft.MultipleChoice -> {
                    MultipleChoiceQuestionCard(
                        title = "Multiple choice ${index + 1}",
                        prompt = item.prompt,
                        rationale = item.rationale,
                        choices = item.choices,
                        selectedAnswers = setOf(item.correctIndex),
                        allowRemove = true,
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
                        allowRemove = true,
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
                        allowRemove = true,
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
private fun InteractiveActivitiesSection(
    topicId: String,
    activities: List<InteractiveDraft>,
    viewModel: ModuleBuilderViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Interactive assessments (Kahoot-style)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        activities.forEachIndexed { index, activity ->
            val allowRemove = activities.size > 1
            when (activity) {
                is QuizInteractiveDraft -> InteractiveQuizCard(
                    title = "Quiz ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as QuizInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as QuizInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onChoiceChange = { optionIndex, value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            val updated = (it as QuizInteractiveDraft).options.toMutableList().apply { set(optionIndex, value) }
                            it.copy(options = updated)
                        }
                    },
                    onToggleAnswer = { answerIndex ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            val current = it as QuizInteractiveDraft
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
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            val current = it as QuizInteractiveDraft
                            val normalized = if (allowMultiple) {
                                current.correctAnswers
                            } else {
                                setOf(current.correctAnswers.firstOrNull() ?: 0)
                            }
                            current.copy(allowMultiple = allowMultiple, correctAnswers = normalized)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
                is TrueFalseInteractiveDraft -> InteractiveTrueFalseCard(
                    title = "True/False ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as TrueFalseInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as TrueFalseInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onAnswerChange = { isTrue ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as TrueFalseInteractiveDraft).copy(correctAnswer = isTrue)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
                is TypeAnswerInteractiveDraft -> InteractiveTypeAnswerCard(
                    title = "Type Answer ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as TypeAnswerInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as TypeAnswerInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onAnswerChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as TypeAnswerInteractiveDraft).copy(answer = value)
                        }
                    },
                    onMaxCharactersChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as TypeAnswerInteractiveDraft).copy(maxCharacters = value)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
                is PuzzleInteractiveDraft -> InteractivePuzzleCard(
                    title = "Puzzle ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as PuzzleInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as PuzzleInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onUpdateBlocks = { blocks ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as PuzzleInteractiveDraft).copy(blocks = blocks)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
                is SliderInteractiveDraft -> InteractiveSliderCard(
                    title = "Slider ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as SliderInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as SliderInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onMinChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as SliderInteractiveDraft).copy(minValue = value)
                        }
                    },
                    onMaxChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as SliderInteractiveDraft).copy(maxValue = value)
                        }
                    },
                    onTargetChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as SliderInteractiveDraft).copy(targetValue = value)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
                is PollInteractiveDraft -> InteractivePollCard(
                    title = "Poll ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as PollInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as PollInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onChoiceChange = { optionIndex, value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            val updated = (it as PollInteractiveDraft).options.toMutableList().apply { set(optionIndex, value) }
                            it.copy(options = updated)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
                is WordCloudInteractiveDraft -> InteractiveWordCloudCard(
                    title = "Word Cloud ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as WordCloudInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as WordCloudInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onMaxWordsChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as WordCloudInteractiveDraft).copy(maxWords = value)
                        }
                    },
                    onMaxCharactersChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as WordCloudInteractiveDraft).copy(maxCharacters = value)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
                is OpenEndedInteractiveDraft -> InteractiveOpenEndedCard(
                    title = "Open Response ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as OpenEndedInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as OpenEndedInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onMaxCharactersChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as OpenEndedInteractiveDraft).copy(maxCharacters = value)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
                is BrainstormInteractiveDraft -> InteractiveBrainstormCard(
                    title = "Brainstorm ${index + 1}",
                    draft = activity,
                    allowRemove = allowRemove,
                    onTitleChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as BrainstormInteractiveDraft).copy(title = value)
                        }
                    },
                    onPromptChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as BrainstormInteractiveDraft).copy(prompt = value)
                        }
                    },
                    onUpdateCategories = { categories ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as BrainstormInteractiveDraft).copy(categories = categories)
                        }
                    },
                    onVoteLimitChange = { value ->
                        viewModel.updateInteractiveActivity(topicId, activity.id) {
                            (it as BrainstormInteractiveDraft).copy(voteLimit = value)
                        }
                    },
                    onRemove = { viewModel.removeInteractiveActivity(topicId, activity.id) }
                )
            }
        }
        AddInteractiveActivityButton(topicId = topicId, viewModel = viewModel)
    }
}

@Composable
private fun AddInteractiveActivityButton(
    topicId: String,
    viewModel: ModuleBuilderViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Add more interactive formats",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add interactive activity")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                InteractiveDraftType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            expanded = false
                            viewModel.addInteractiveActivity(topicId, type)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveQuizCard(
    title: String,
    draft: QuizInteractiveDraft,
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
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "Quiz")
                InfoPill(text = "Scored")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt or question") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (draft.allowMultiple) "Multiple answers allowed" else "Single answer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = draft.allowMultiple, onCheckedChange = onAllowMultipleChanged)
            }
            MultipleChoiceOptionsEditor(
                choices = draft.options,
                selectedIndices = draft.correctAnswers,
                onChoiceChange = onChoiceChange,
                onSelectionChange = onToggleAnswer
            )
        }
    }
}

@Composable
private fun InteractiveTrueFalseCard(
    title: String,
    draft: TrueFalseInteractiveDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
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
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove true/false")
                    }
                }
            }
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "True/False")
                InfoPill(text = "Scored")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = draft.correctAnswer,
                    onClick = { onAnswerChange(true) },
                    label = { Text("TRUE") }
                )
                FilterChip(
                    selected = !draft.correctAnswer,
                    onClick = { onAnswerChange(false) },
                    label = { Text("FALSE") }
                )
            }
        }
    }
}

@Composable
private fun InteractiveTypeAnswerCard(
    title: String,
    draft: TypeAnswerInteractiveDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onMaxCharactersChange: (String) -> Unit,
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
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove type answer")
                    }
                }
            }
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "Type Answer")
                InfoPill(text = "Scored")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.answer,
                onValueChange = onAnswerChange,
                label = { Text("Correct answer") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.maxCharacters,
                onValueChange = onMaxCharactersChange,
                label = { Text("Max characters") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
private fun InteractivePuzzleCard(
    title: String,
    draft: PuzzleInteractiveDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onUpdateBlocks: (List<String>) -> Unit,
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
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove puzzle")
                    }
                }
            }
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "Puzzle")
                InfoPill(text = "Scored")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Correct order of blocks",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            draft.blocks.forEachIndexed { index, block ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = block,
                        onValueChange = { value ->
                            val updated = draft.blocks.toMutableList().apply { set(index, value) }
                            onUpdateBlocks(updated)
                        },
                        label = { Text("Block ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    if (draft.blocks.size > 2) {
                        IconButton(onClick = {
                            val updated = draft.blocks.toMutableList().apply { removeAt(index) }
                            onUpdateBlocks(updated)
                        }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove block")
                        }
                    }
                }
            }
            TextButton(onClick = {
                val nextIndex = draft.blocks.size + 1
                onUpdateBlocks(draft.blocks + "Step $nextIndex")
            }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add block")
            }
        }
    }
}

@Composable
private fun InteractiveSliderCard(
    title: String,
    draft: SliderInteractiveDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
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
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove slider")
                    }
                }
            }
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "Slider")
                InfoPill(text = "Scored")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.minValue,
                    onValueChange = onMinChange,
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = draft.maxValue,
                    onValueChange = onMaxChange,
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = draft.targetValue,
                    onValueChange = onTargetChange,
                    label = { Text("Target") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
private fun InteractivePollCard(
    title: String,
    draft: PollInteractiveDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onChoiceChange: (Int, String) -> Unit,
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
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove poll")
                    }
                }
            }
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "Poll")
                InfoPill(text = "No points")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            MultipleChoiceOptionsEditor(
                choices = draft.options,
                selectedIndices = emptySet(),
                onChoiceChange = onChoiceChange,
                onSelectionChange = {},
                showSelection = false
            )
        }
    }
}

@Composable
private fun InteractiveWordCloudCard(
    title: String,
    draft: WordCloudInteractiveDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onMaxWordsChange: (String) -> Unit,
    onMaxCharactersChange: (String) -> Unit,
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
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove word cloud")
                    }
                }
            }
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "Word Cloud")
                InfoPill(text = "No points")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.maxWords,
                    onValueChange = onMaxWordsChange,
                    label = { Text("Max words") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = draft.maxCharacters,
                    onValueChange = onMaxCharactersChange,
                    label = { Text("Max characters") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
private fun InteractiveOpenEndedCard(
    title: String,
    draft: OpenEndedInteractiveDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onMaxCharactersChange: (String) -> Unit,
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
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove open response")
                    }
                }
            }
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "Open Response")
                InfoPill(text = "No points")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.maxCharacters,
                onValueChange = onMaxCharactersChange,
                label = { Text("Max characters") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
private fun InteractiveBrainstormCard(
    title: String,
    draft: BrainstormInteractiveDraft,
    allowRemove: Boolean,
    onTitleChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onUpdateCategories: (List<String>) -> Unit,
    onVoteLimitChange: (String) -> Unit,
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
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.weight(1f))
                if (allowRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove brainstorm")
                    }
                }
            }
            AdaptiveWrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                InfoPill(text = "Brainstorm")
                InfoPill(text = "No points")
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Idea buckets",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            draft.categories.forEachIndexed { index, category ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { value ->
                            val updated = draft.categories.toMutableList().apply { set(index, value) }
                            onUpdateCategories(updated)
                        },
                        label = { Text("Category ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    if (draft.categories.size > 2) {
                        IconButton(onClick = {
                            val updated = draft.categories.toMutableList().apply { removeAt(index) }
                            onUpdateCategories(updated)
                        }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove category")
                        }
                    }
                }
            }
            TextButton(onClick = {
                val nextIndex = draft.categories.size + 1
                onUpdateCategories(draft.categories + "Idea $nextIndex")
            }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add category")
            }
            OutlinedTextField(
                value = draft.voteLimit,
                onValueChange = onVoteLimitChange,
                label = { Text("Votes per player") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
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
private fun MultipleChoiceOptionsEditor(
    choices: List<String>,
    selectedIndices: Set<Int>,
    onChoiceChange: (Int, String) -> Unit,
    onSelectionChange: (Int) -> Unit,
    showSelection: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEachIndexed { index, choice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showSelection) {
                    val selected = index in selectedIndices
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectionChange(index) },
                        label = { Text(optionLabel(index)) }
                    )
                } else {
                    Text(
                        text = optionLabel(index),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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




