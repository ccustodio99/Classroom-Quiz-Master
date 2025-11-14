package com.classroom.quizmaster.ui.teacher.quiz_editor

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.ui.components.DiscardDraftDialog
import com.classroom.quizmaster.ui.components.PrimaryButton
import com.classroom.quizmaster.ui.components.SaveChangesDialog
import com.classroom.quizmaster.ui.components.SecondaryButton
import com.classroom.quizmaster.ui.components.TagChip
import com.classroom.quizmaster.ui.components.ToggleChip
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = if (state.isNewQuiz) "Create quiz" else "Edit quiz", style = MaterialTheme.typography.headlineMedium)
        ClassroomTopicSelector(
            classroomOptions = state.classroomOptions,
            selectedClassroomId = state.classroomId,
            onClassroomSelected = onClassroomChange,
            topicOptions = state.topicsByClassroom[state.classroomId].orEmpty(),
            selectedTopicId = state.topicId,
            onTopicSelected = onTopicChange
        )
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Time/question: ${state.timePerQuestionSeconds}s")
            Slider(
                value = state.timePerQuestionSeconds.toFloat(),
                onValueChange = { onTimeChange(it.toInt()) },
                valueRange = 15f..120f,
                modifier = Modifier.weight(1f)
            )
        }
        ToggleChip(
            label = "Shuffle questions",
            checked = state.shuffleQuestions,
            onCheckedChange = onShuffleChange,
            description = "Randomize order per student"
        )
        Text(text = "Questions", style = MaterialTheme.typography.titleLarge)
        QuestionList(
            questions = state.questions,
            onStemChange = onQuestionStem,
            onAnswerChange = onAnswerChange,
            onToggleCorrect = onToggleCorrect,
            onExplanationChange = onExplanationChange,
            onReorderQuestion = onReorderQuestion
        )
        AddQuestionRow(onAddQuestion = onAddQuestion)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PrimaryButton(
                text = "Save",
                onClick = onSaveClick,
                enabled = state.classroomId.isNotBlank() && state.topicId.isNotBlank()
            )
            SecondaryButton(text = "Discard", onClick = onDiscardClick)
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

@OptIn(ExperimentalMaterial3Api::class)
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

        val classroomExpanded = remember { mutableStateOf(false) }
        val classroomLabel = classroomOptions.firstOrNull { it.id == selectedClassroomId }
        ExposedDropdownMenuBox(
            expanded = classroomExpanded.value,
            onExpandedChange = { classroomExpanded.value = !classroomExpanded.value }
        ) {
            OutlinedTextField(
                value = classroomLabel?.label.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Classroom") },
                placeholder = { Text("Select a classroom") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = classroomExpanded.value)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(
                expanded = classroomExpanded.value,
                onDismissRequest = { classroomExpanded.value = false }
            ) {
                classroomOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(option.label)
                                if (option.supportingText.isNotBlank()) {
                                    Text(
                                        text = option.supportingText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onClassroomSelected(option.id)
                            classroomExpanded.value = false
                        }
                    )
                }
            }
        }

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
            val topicExpanded = remember { mutableStateOf(false) }
            val topicLabel = resolvedTopics.firstOrNull { it.id == selectedTopicId }
            ExposedDropdownMenuBox(
                expanded = topicExpanded.value,
                onExpandedChange = { topicExpanded.value = !topicExpanded.value }
            ) {
                OutlinedTextField(
                    value = topicLabel?.label.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Topic") },
                    placeholder = { Text("Select a topic") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicExpanded.value)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                DropdownMenu(
                    expanded = topicExpanded.value,
                    onDismissRequest = { topicExpanded.value = false }
                ) {
                    resolvedTopics.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(option.label)
                                    if (option.supportingText.isNotBlank()) {
                                        Text(
                                            text = option.supportingText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onTopicSelected(option.id)
                                topicExpanded.value = false
                            }
                        )
                    }
                }
            }
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
        TagChip(text = "No questions yet")
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
                    TextButton(onClick = { /* media picker placeholder */ }) {
                        Text("Attach media (UI only)")
                    }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Add question", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuestionTypeUi.values().forEach { type ->
                SecondaryButton(text = "+ ${type.name}", onClick = { onAddQuestion(type) })
            }
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
