package com.classroom.quizmaster.ui.feature.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.TrueFalseItem
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.InfoPill
import com.classroom.quizmaster.ui.components.SectionCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeliveryScreen(
    viewModel: DeliveryViewModel,
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val stage = state.stage
    val stageMeta = remember(stage) { stage.toMeta() }

    GenZScaffold(
        title = "Live delivery",
        subtitle = stageMeta.subtitle,
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StageProgress(meta = stageMeta)

            when (stage) {
                Stage.Loading -> LoadingStage()
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
                is Stage.Summary -> SummaryStage(
                    stage = stage,
                    onDone = onFinished
                )
            }
        }
    }
}

@Composable
private fun StageProgress(meta: StageMeta) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoPill(text = meta.badgeLabel)
            Text(
                text = meta.headline,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = meta.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { meta.progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun LoadingStage() {
    SectionCard(
        title = "Warming up",
        subtitle = "Syncing module assets",
        caption = "We’re fetching the latest assessment keys and lesson cards."
    ) {
        Text(
            text = "Preparing content...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CaptureStudentStage(
    name: String,
    onNameChange: (String) -> Unit,
    onStart: () -> Unit
) {
    SectionCard(
        title = "Welcome the learner",
        subtitle = "Capture nickname to personalise analytics",
        caption = "We keep data on-device. Nicknames help surface badges and growth moments."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nickname") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.Send, contentDescription = null)
                Text("Simulan ang Pagsusulit Bago ang Aralin", modifier = Modifier.padding(start = 8.dp))
            }
        }
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
    val progressLabel = "Tanong ${stage.currentIndex + 1} / ${stage.questions.size}"
    SectionCard(
        title = if (stage.kind == AssessmentKind.PRE) "Pagsusulit Bago ang Aralin" else "Pagsusulit Pagkatapos ng Aralin",
        subtitle = progressLabel,
        caption = question.item.prompt
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (val item = question.item) {
                is MultipleChoiceItem -> {
                    val selectedIndex = question.answer.toIntOrNull()
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item.choices.forEachIndexed { index, choice ->
                            val isSelected = selectedIndex == index
                            FilledTonalButton(
                                onClick = { onAnswerChange(stage.currentIndex, index.toString()) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(choice)
                            }
                        }
                    }
                }
                is TrueFalseItem -> {
                    val answer = question.answer
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = { onAnswerChange(stage.currentIndex, "true") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (answer == "true") MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (answer == "true") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Tama")
                        }
                        FilledTonalButton(
                            onClick = { onAnswerChange(stage.currentIndex, "false") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (answer == "false") MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (answer == "false") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Mali")
                        }
                    }
                }
                is NumericItem -> {
                    OutlinedTextField(
                        value = question.answer,
                        onValueChange = { onAnswerChange(stage.currentIndex, it) },
                        label = { Text("Sagot") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is MatchingItem -> {
                    Text(
                        "I-type ang pares gaya ng 'A->B;C->D'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = question.answer,
                        onValueChange = { onAnswerChange(stage.currentIndex, it) },
                        label = { Text("Mga Pares") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "Paliwanag: ${question.item.explanation}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    enabled = stage.currentIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Nakaraan")
                }
                if (stage.currentIndex < stage.questions.lastIndex) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Susunod")
                    }
                } else {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                        Text("Isumite", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonStageView(
    stage: Stage.LessonStage,
    onSubmitCheck: (String) -> Unit,
    onNextSlide: () -> Unit
) {
    SectionCard(
        title = "Talakayan ${stage.slideIndex}/${stage.totalSlides}",
        subtitle = stage.slideTitle,
        caption = "Keep momentum with quick reveals and interactive prompts."
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stage.slideContent,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            stage.miniCheckPrompt?.let { prompt ->
                val answerState = remember(stage.slideIndex) { mutableStateOf(stage.miniCheckAnswer) }
                LaunchedEffect(stage.miniCheckAnswer, stage.slideIndex) {
                    answerState.value = stage.miniCheckAnswer
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    tonalElevation = 0.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Mini Check: $prompt", fontWeight = FontWeight.SemiBold)
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
                                text = if (correct) "Tama!" else "Subukan muli.",
                                color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            Button(onClick = onNextSlide, modifier = Modifier.fillMaxWidth()) {
                Text(if (stage.finished) "Pumunta sa Post-Test" else "Susunod na Slide")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryStage(
    stage: Stage.Summary,
    onDone: () -> Unit
) {
    SectionCard(
        title = "Learning gains",
        subtitle = "Pag-angat ng Marka",
        caption = "Celebrate growth and capture badges for motivation."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryScores(pre = stage.pre, post = stage.post)
            stage.report?.let { report ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Objective mastery", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    report.mastery.values.forEach { mastery ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 0.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(mastery.objective, fontWeight = FontWeight.Medium)
                                LinearProgressIndicator(
                                    progress = { (mastery.post / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                    trackColor = MaterialTheme.colorScheme.background
                                )
                                Text(
                                    text = "Pre ${"%.1f".format(mastery.pre)}% → Post ${"%.1f".format(mastery.post)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            if (stage.badges.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Badges unlocked", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stage.badges.forEach { badge ->
                            InfoPill(text = badge.title)
                        }
                    }
                }
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Tapusin")
            }
        }
    }
}

@Composable
private fun SummaryScores(pre: Double, post: Double) {
    val gain = (post - pre).roundToInt()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pre ${"%.1f".format(pre)}% → Post ${"%.1f".format(post)}%", fontWeight = FontWeight.SemiBold)
            Text(
                text = if (gain >= 0) "▲ +$gain pts" else "▼ ${gain.absoluteValue} pts",
                style = MaterialTheme.typography.bodyLarge,
                color = if (gain >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

private data class StageMeta(
    val headline: String,
    val subtitle: String,
    val badgeLabel: String,
    val progress: Float
)

private fun Stage.toMeta(): StageMeta = when (this) {
    Stage.Loading -> StageMeta(
        headline = "Syncing module",
        subtitle = "Hold tight while we prep your Gen Z flow",
        badgeLabel = "Preparing",
        progress = 0.08f
    )
    Stage.CaptureStudent -> StageMeta(
        headline = "Welcome the learner",
        subtitle = "Capture a nickname to personalise reports",
        badgeLabel = "Onboarding",
        progress = 0.16f
    )
    is Stage.AssessmentStage -> {
        val base = if (kind == AssessmentKind.PRE) 0.22f else 0.78f
        val range = 0.18f
        val ratio = if (questions.isNotEmpty()) (currentIndex + 1f) / questions.size else 0f
        StageMeta(
            headline = if (kind == AssessmentKind.PRE) "Pre-Test diagnostics" else "Post-Test reflection",
            subtitle = "Tanong ${currentIndex + 1} / ${questions.size}",
            badgeLabel = if (kind == AssessmentKind.PRE) "Pre-Test" else "Post-Test",
            progress = (base + ratio * range).coerceIn(0f, 0.94f)
        )
    }
    is Stage.LessonStage -> {
        val ratio = if (totalSlides > 0) slideIndex.toFloat() / totalSlides else 0f
        StageMeta(
            headline = "Talakayan live",
            subtitle = "Slide $slideIndex / $totalSlides",
            badgeLabel = "Talakayan",
            progress = (0.46f + ratio * 0.24f).coerceIn(0f, 0.9f)
        )
    }
    is Stage.Summary -> StageMeta(
        headline = "Learning gains",
        subtitle = "Pag-angat ng marka at badges",
        badgeLabel = "Summary",
        progress = 1f
    )
}

private val Int.absoluteValue: Int get() = kotlin.math.abs(this)
