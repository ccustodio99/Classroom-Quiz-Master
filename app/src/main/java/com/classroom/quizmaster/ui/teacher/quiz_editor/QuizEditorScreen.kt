package com.classroom.quizmaster.ui.teacher.quiz_editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.DiscardDraftDialog
import com.classroom.quizmaster.ui.components.DropdownField
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SaveChangesDialog
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.components.ToggleChip
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.QuizCategoryUi
import com.classroom.quizmaster.ui.model.SelectionOptionUi
import com.classroom.quizmaster.ui.preview.QuizPreviews
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

@Composable
fun QuizEditorRoute(
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
    viewModel: QuizEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    QuizEditorScreen(
        state = state,
        onTitleChange = viewModel::updateTitle,
        onClassroomChange = viewModel::updateClassroom,
        onTopicChange = viewModel::updateTopic,
        onGradeChange = viewModel::updateGrade,
        onSubjectChange = viewModel::updateSubject,
        onTimeChange = viewModel::updateTimePerQuestion,
        onShuffleChange = viewModel::toggleShuffle,
        onCategoryChange = viewModel::updateCategory,
        onAddQuestion = viewModel::addQuestion,
        onQuestionStem = viewModel::updateQuestionStem,
        onAnswerChange = viewModel::updateAnswerText,
        onToggleCorrect = viewModel::toggleCorrectAnswer,
        onExplanationChange = viewModel::updateExplanation,
        onReorderQuestion = viewModel::reorderQuestion,
        onSaveClick = { viewModel.showSaveDialog(true) },
        onDiscardClick = { viewModel.showDiscardDialog(true) },
        onDiscardConfirmed = {
            viewModel.showDiscardDialog(false)
            onDiscarded()
        },
        onConfirmSave = {
            viewModel.persist(onSaved)
        },
        onDismissSaveDialog = { viewModel.showSaveDialog(false) },
        onDismissDiscardDialog = { viewModel.showDiscardDialog(false) }
    )
}

@Composable
fun QuizEditorScreen(
    state: QuizEditorUiState,
    onTitleChange: (String) -> Unit,
    onClassroomChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onTimeChange: (Int) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onCategoryChange: (QuizCategoryUi) -> Unit,
    onAddQuestion: (QuestionTypeUi) -> Unit,
    onQuestionStem: (String, String) -> Unit,
    onAnswerChange: (String, String, String) -> Unit,
    onToggleCorrect: (String, String) -> Unit,
    onExplanationChange: (String, String) -> Unit,
    onReorderQuestion: (Int, Int) -> Unit,
    onSaveClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onDiscardConfirmed: () -> Unit,
    onConfirmSave: () -> Unit,
    onDismissSaveDialog: () -> Unit,
    onDismissDiscardDialog: () -> Unit
) {
    val verticalSpacing = 16.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        HeaderSummary(state = state)

        SectionCard(
            title = "Assignment context",
            subtitle = "Pair the quiz to where students will see it",
            leadingIcon = Icons.Outlined.School
        ) {
            ClassroomTopicSelector(
                classroomOptions = state.classroomOptions,
                selectedClassroomId = state.classroomId,
                onClassroomSelected = onClassroomChange,
                topicOptions = state.topicsByClassroom[state.classroomId].orEmpty(),
                selectedTopicId = state.topicId,
                onTopicSelected = onTopicChange
            )
            SectionHeader(
                text = "Test type",
                helper = state.quizCategory.description
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizCategoryUi.entries.forEach { category ->
                    FilterChip(
                        selected = state.quizCategory == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category.displayName) }
                    )
                }
            }
        }

        SectionCard(
            title = "Quiz basics",
            subtitle = "Set the essentials so learners have context",
            leadingIcon = Icons.Outlined.Info
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.grade,
                    onValueChange = onGradeChange,
                    label = { Text("Grade") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = onSubjectChange,
                    label = { Text("Subject") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionCard(
            title = "Timing & flow",
            subtitle = "Match pacing to difficulty and keep sessions fair",
            leadingIcon = Icons.Outlined.Schedule
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Time per question", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${state.timePerQuestionSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = state.timePerQuestionSeconds.toFloat(),
                onValueChange = { onTimeChange(it.toInt()) },
                valueRange = 15f..120f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "15s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "120s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ToggleChip(
                label = "Shuffle questions",
                checked = state.shuffleQuestions,
                onCheckedChange = onShuffleChange,
                description = "Randomize order per student"
            )
        }

        SectionCard(
            title = "Questions",
            subtitle = "Build, reorder, and give helpful explanations",
            leadingIcon = Icons.Outlined.Topic
        ) {
            QuestionList(
                questions = state.questions,
                onStemChange = onQuestionStem,
                onAnswerChange = onAnswerChange,
                onToggleCorrect = onToggleCorrect,
                onExplanationChange = onExplanationChange,
                onReorderQuestion = onReorderQuestion
            )
            AddQuestionRow(onAddQuestion = onAddQuestion)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PrimaryButton(
                text = "Save",
                onClick = onSaveClick,
                enabled = state.classroomId.isNotBlank() && state.topicId.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Discard",
                onClick = onDiscardClick,
                modifier = Modifier.weight(1f)
            )
        }
        SaveChangesDialog(
            open = state.showSaveDialog,
            onDismiss = onDismissSaveDialog,
            onSave = onConfirmSave,
            onDiscard = onDiscardConfirmed
        )
        DiscardDraftDialog(
            open = state.showDiscardDialog,
            onDismiss = onDismissDiscardDialog,
            onConfirm = onDiscardConfirmed
        )
    }
}

private fun QuizEditorUiState.headerTitle(): String = when {
    isNewQuiz -> when (quizCategory) {
        QuizCategoryUi.Standard -> "Create quiz"
        QuizCategoryUi.PreTest -> "Create pre-test"
        QuizCategoryUi.PostTest -> "Create post-test"
    }
    else -> when (quizCategory) {
        QuizCategoryUi.Standard -> "Edit quiz"
        QuizCategoryUi.PreTest -> "Edit pre-test"
        QuizCategoryUi.PostTest -> "Edit post-test"
    }
}

@Composable
private fun ClassroomTopicSelector(
    classroomOptions: List<SelectionOptionUi>,
    selectedClassroomId: String,
    onClassroomSelected: (String) -> Unit,
    topicOptions: List<SelectionOptionUi>,
    selectedTopicId: String,
    onTopicSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (classroomOptions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "No classrooms found", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Create a classroom before assigning quizzes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        DropdownField(
            label = "Classroom",
            items = classroomOptions,
            selectedItem = classroomOptions.firstOrNull { option -> option.id == selectedClassroomId },
            onItemSelected = { option -> onClassroomSelected(option.id) },
            itemLabel = { option -> option.label }
        )

        val resolvedTopics = topicOptions
        if (resolvedTopics.isEmpty()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                enabled = false,
                label = { Text("Topic") },
                placeholder = { Text("Add a topic in this classroom") },
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = "Topics keep quizzes organized. Create one from the classroom view.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            DropdownField(
                label = "Topic",
                items = resolvedTopics,
                selectedItem = resolvedTopics.firstOrNull { option -> option.id == selectedTopicId },
                onItemSelected = { option -> onTopicSelected(option.id) },
                itemLabel = { option -> option.label }
            )
        }
    }
}

@Composable
private fun QuestionList(
    questions: List<QuestionDraftUi>,
    onStemChange: (String, String) -> Unit,
    onAnswerChange: (String, String, String) -> Unit,
    onToggleCorrect: (String, String) -> Unit,
    onExplanationChange: (String, String) -> Unit,
    onReorderQuestion: (Int, Int) -> Unit
) {
    if (questions.isEmpty()) {
        EmptyQuestionsState()
    } else {
        questions.forEachIndexed { index, question ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Q${index + 1}: ${question.type.name}", style = MaterialTheme.typography.titleMedium)
                        Row {
                            IconButton(
                                onClick = { onReorderQuestion(index, index - 1) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move question up"
                                )
                            }
                            IconButton(
                                onClick = { onReorderQuestion(index, index + 1) },
                                enabled = index < questions.lastIndex
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move question down"
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = question.stem,
                        onValueChange = { onStemChange(question.id, it) },
                        label = { Text("Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    question.answers.forEach { answer ->
                        AnswerRow(
                            questionId = question.id,
                            answer = answer,
                            onAnswerChange = onAnswerChange,
                            onToggleCorrect = onToggleCorrect
                        )
                    }
                    OutlinedTextField(
                        value = question.explanation,
                        onValueChange = { onExplanationChange(question.id, it) },
                        label = { Text("Explanation (shown after reveal)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val mediaLabel = question.mediaThumb?.takeIf { it.isNotBlank() }
                        ?.let { "Attached media: $it" }
                        ?: "Add media from your materials library before hosting"
                    Text(mediaLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AnswerRow(
    questionId: String,
    answer: AnswerOptionUi,
    onAnswerChange: (String, String, String) -> Unit,
    onToggleCorrect: (String, String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(
            checked = answer.correct,
            onCheckedChange = { onToggleCorrect(questionId, answer.id) }
        )
        OutlinedTextField(
            value = answer.text,
            onValueChange = { onAnswerChange(questionId, answer.id, it) },
            label = { Text("Answer ${answer.label}") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AddQuestionRow(onAddQuestion: (QuestionTypeUi) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(text = "Add question", helper = "Mix question types to balance rigor")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuestionTypeUi.values()
                .filterNot { it == QuestionTypeUi.Match }
                .forEach { type ->
                QuestionTypeCard(
                    type = type,
                    icon = when (type) {
                        QuestionTypeUi.MultipleChoice -> Icons.Outlined.ViewList
                        QuestionTypeUi.TrueFalse -> Icons.Outlined.ToggleOn
                        QuestionTypeUi.FillIn -> Icons.Outlined.ViewList
                        else -> Icons.Outlined.ViewList
                    },
                    onClick = { onAddQuestion(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuestionTypeCard(
    type: QuestionTypeUi,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = when (type) {
                        QuestionTypeUi.MultipleChoice -> "Multiple choice"
                        QuestionTypeUi.TrueFalse -> "True/False"
                        QuestionTypeUi.FillIn -> "Fill in the blank"
                        else -> "Question"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (type) {
                        QuestionTypeUi.MultipleChoice -> "Great for recall and quick checks"
                        QuestionTypeUi.TrueFalse -> "Use for binary concepts and quick pacing"
                        QuestionTypeUi.FillIn -> "Short text answers for precision recall"
                        else -> "Use the right type to fit your assessment"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeaderSummary(state: QuizEditorUiState) {
    Surface(
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = state.headerTitle(),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Craft a clear, paced experience that aligns with your classroom and topic.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagChip(
                    text = if (state.classroomId.isNotBlank()) "Classroom selected" else "Choose classroom"
                )
                TagChip(
                    text = if (state.topicId.isNotBlank()) "Topic selected" else "Choose topic"
                )
                TagChip(text = state.quizCategory.displayName)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                text = title,
                helper = subtitle,
                icon = leadingIcon
            )
            content()
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    helper: String? = null,
    icon: ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
            helper?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyQuestionsState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "No questions yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Add a MultipleChoice or TrueFalse item to get started.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@QuizPreviews
@Composable
private fun QuizEditorPreview() {
    QuizMasterTheme {
        QuizEditorScreen(
            state = QuizEditorUiState(
                classroomId = "room1",
                topicId = "topic1",
                title = "Fractions review",
                grade = "4",
                subject = "Math",
                questions = listOf(
                    QuestionDraftUi(
                        id = "q1",
                        stem = "What is 1/2 + 1/4?",
                        type = QuestionTypeUi.MultipleChoice,
                        answers = listOf(
                            AnswerOptionUi("a1", "A", "3/4", true),
                            AnswerOptionUi("a2", "B", "2/4", false)
                        ),
                        explanation = "Convert to like denominators."
                    )
                ),
                quizCategory = QuizCategoryUi.PreTest,
                classroomOptions = listOf(
                    SelectionOptionUi("room1", "Period 1 Algebra", "Grade 4 • Math"),
                    SelectionOptionUi("room2", "STEM Club", "Grade 5 • Science")
                ),
                topicsByClassroom = mapOf(
                    "room1" to listOf(
                        SelectionOptionUi("topic1", "Fractions", "Number sense"),
                        SelectionOptionUi("topic2", "Decimals", "Place value")
                    ),
                    "room2" to listOf(
                        SelectionOptionUi("topic3", "Space", "Earth & Space"),
                        SelectionOptionUi("topic4", "Robotics", "STEM challenges")
                    )
                )
            ),
            onTitleChange = {},
            onClassroomChange = {},
            onTopicChange = {},
            onGradeChange = {},
            onSubjectChange = {},
            onTimeChange = {},
            onShuffleChange = {},
            onCategoryChange = {},
            onAddQuestion = {},
            onQuestionStem = { _, _ -> },
            onAnswerChange = { _, _, _ -> },
            onToggleCorrect = { _, _ -> },
            onExplanationChange = { _, _ -> },
            onReorderQuestion = { _, _ -> },
            onSaveClick = {},
            onDiscardClick = {},
            onDiscardConfirmed = {},
            onConfirmSave = {},
            onDismissSaveDialog = {},
            onDismissDiscardDialog = {}
        )
    }
}
