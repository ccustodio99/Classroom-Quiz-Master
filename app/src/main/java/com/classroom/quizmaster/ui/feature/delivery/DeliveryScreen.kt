package com.classroom.quizmaster.ui.feature.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.TrueFalseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(
    viewModel: DeliveryViewModel,
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Delivery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val stage = state.stage) {
                Stage.Loading -> Text("Loading...")
                Stage.CaptureStudent -> CaptureStudentStage(
                    name = state.studentName,
                    onNameChange = viewModel::onStudentNameChanged,
                    onStart = viewModel::beginPreTest
                )
                is Stage.AssessmentStage -> AssessmentStageView(
                    stage = stage,
                    onAnswerChange = viewModel::updateAnswer,
                    onNext = viewModel::nextQuestion,
                    onPrev = viewModel::previousQuestion,
                    onSubmit = viewModel::submitAssessment
                )
                is Stage.LessonStage -> LessonStageView(
                    stage = stage,
                    onSubmitCheck = viewModel::submitMiniCheck,
                    onNextSlide = viewModel::goToNextLessonStep
                )
                is Stage.Summary -> SummaryStage(stage = stage, onDone = onFinished)
            }
        }
    }
}

@Composable
private fun CaptureStudentStage(name: String, onNameChange: (String) -> Unit, onStart: () -> Unit) {
    Text("Pangalan ng Mag-aaral")
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Nickname") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Send, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Simulan ang Pagsusulit Bago ang Aralin")
    }
}

@Composable
private fun AssessmentStageView(
    stage: Stage.AssessmentStage,
    onAnswerChange: (Int, String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSubmit: () -> Unit
) {
    val question = stage.questions[stage.currentIndex]
    Text("Tanong ${stage.currentIndex + 1} / ${stage.questions.size}", style = MaterialTheme.typography.titleMedium)
    AssessmentQuestionCard(question = question, onAnswer = { answer -> onAnswerChange(stage.currentIndex, answer) })
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Button(onClick = onPrev, enabled = stage.currentIndex > 0) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nakaraan")
        }
        if (stage.currentIndex < stage.questions.lastIndex) {
            Button(onClick = onNext) {
                Text("Susunod")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        } else {
            Button(onClick = onSubmit) {
                Text("Isumite")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

@Composable
private fun AssessmentQuestionCard(question: QuestionState, onAnswer: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(question.item.prompt, style = MaterialTheme.typography.titleMedium)
            when (val item = question.item) {
                is MultipleChoiceItem -> {
                    item.choices.forEachIndexed { index, choice ->
                        Button(
                            onClick = { onAnswer(index.toString()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(choice)
                        }
                    }
                }
                is TrueFalseItem -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onAnswer("true") }) { Text("Tama") }
                        Button(onClick = { onAnswer("false") }) { Text("Mali") }
                    }
                }
                is NumericItem -> {
                    OutlinedTextField(
                        value = question.answer,
                        onValueChange = onAnswer,
                        label = { Text("Sagot") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                is MatchingItem -> {
                    Text("I-type ang pares gaya ng 'A->B;C->D'")
                    OutlinedTextField(
                        value = question.answer,
                        onValueChange = onAnswer,
                        label = { Text("Mga Pares") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Text("Paliwanag: ${question.item.explanation}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LessonStageView(
    stage: Stage.LessonStage,
    onSubmitCheck: (String) -> Unit,
    onNextSlide: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Talakayan ${stage.slideIndex}/${stage.totalSlides}", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stage.slideTitle, style = MaterialTheme.typography.titleLarge)
                Text(stage.slideContent)
                stage.miniCheckPrompt?.let {
                    Text("Mini Check: $it")
                    val answerState = remember(stage.slideIndex) { mutableStateOf(stage.miniCheckAnswer) }
                    LaunchedEffect(stage.miniCheckAnswer, stage.slideIndex) {
                        answerState.value = stage.miniCheckAnswer
                    }
                    OutlinedTextField(
                        value = answerState.value,
                        onValueChange = { answerState.value = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { onSubmitCheck(answerState.value) }) {
                        Text("I-check")
                    }
                    stage.miniCheckResult?.let { correct ->
                        Text(
                            if (correct) "Tama!" else "Subukan muli.",
                            color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
                Button(onClick = onNextSlide, modifier = Modifier.fillMaxWidth()) {
                    Text(if (stage.finished) "Pumunta sa Post-Test" else "Susunod na Slide")
                }
            }
        }
    }
}

@Composable
private fun SummaryStage(stage: Stage.Summary, onDone: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pagsusulit Pagkatapos ng Aralin", style = MaterialTheme.typography.titleMedium)
        Text(String.format("Pre: %.1f%%  Post: %.1f%%", stage.pre, stage.post))
        stage.report?.let { report ->
            report.mastery.values.forEach { mastery ->
                Text("${mastery.objective}: Pre ${"%.1f".format(mastery.pre)}% -> Post ${"%.1f".format(mastery.post)}%")
            }
        }
        if (stage.badges.isNotEmpty()) {
            Text("Badges")
            stage.badges.forEach { badge ->
                Text("* ${badge.title} - ${badge.description}")
            }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Tapusin")
        }
    }
}
