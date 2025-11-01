package com.classroom.quizmaster.ui.feature.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.ui.components.GenZScaffold
import com.classroom.quizmaster.ui.components.SectionCard

@Composable
fun HelpGuideScreen(
    onBack: () -> Unit
) {
    val knowledgeTypes = listOf(
        QuestionType("Multiple Choice", "Standard 2–4 options, one or more correct answers."),
        QuestionType("True / False", "Quick factual checks."),
        QuestionType("Numeric Entry", "Students type numeric answers with tolerance settings."),
        QuestionType("Matching", "Pair related terms, formulas, or definitions."),
        QuestionType("Type Answer", "Type a short response (≤20 chars)."),
        QuestionType("Puzzle", "Arrange steps or ideas in correct order."),
        QuestionType("Slider", "Indicate a numeric value or confidence level.")
    )
    val opinionTypes = listOf(
        QuestionType("Poll", "Quick check of opinions or understanding."),
        QuestionType("Word Cloud", "Players submit one word to summarize learning."),
        QuestionType("Open-Ended", "Longer text input for reflections."),
        QuestionType("Brainstorm", "Collaborative idea generation and voting.")
    )

    GenZScaffold(
        title = "Help & Guide",
        subtitle = "Deliver engaging, local-first classroom experiences",
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SectionCard(
                    title = "How Classroom Quiz Master Works",
                    subtitle = "Plan, deliver, and measure lessons in one flow"
                ) {
                    GuideParagraph("Classroom Quiz Master is a teacher-friendly Kotlin Android app that keeps assessments and reports on-device. Use it to deliver structured modules — even offline — while tracking learning gains and participation in real time.")
                    GuideParagraph("Each module follows a Pre-Test → Lesson Activities → Post-Test cycle so you can benchmark learning gains and export results for parents or school leaders.")
                }
            }
            item {
                SectionCard(
                    title = "Teacher Role",
                    subtitle = "Host or create modules with confidence"
                ) {
                    GuideStep(
                        number = 1,
                        title = "Create or import a lesson module",
                        description = "Define objectives, attach slides or media, and configure timers, randomization, and optional leaderboards."
                    )
                    GuideStep(
                        number = 2,
                        title = "Launch a live session",
                        description = "Tap Start Live Delivery to generate a short-lived class code (e.g., 845 209) and mirror your screen to a projector, TV, or screen-share."
                    )
                    GuideStep(
                        number = 3,
                        title = "Monitor and report",
                        description = "Scores, participation, and pre/post gains are stored locally. Export PDF or CSV reports to highlight objective mastery and commonly missed items."
                    )
                }
            }
            item {
                SectionCard(
                    title = "Student Role",
                    subtitle = "Join with any device — no accounts required"
                ) {
                    GuideStep(
                        number = 1,
                        title = "Join the session",
                        description = "Open the join screen, connect to the same Wi-Fi or teacher hotspot, and enter the class code with a nickname or student ID."
                    )
                    GuideStep(
                        number = 2,
                        title = "Play and engage",
                        description = "Watch the shared display for questions and submit answers on personal devices. Scoring balances accuracy and fairness, without speed bonuses during diagnostics."
                    )
                    GuideStep(
                        number = 3,
                        title = "Privacy and reliability",
                        description = "No mandatory accounts or cloud sync. Data stays on the teacher’s device so classes keep running even without internet."
                    )
                }
            }
            item {
                SectionCard(
                    title = "Question Types",
                    subtitle = "Mix diagnostics and opinion-based activities"
                ) {
                    GuideCategoryHeader("To Test Knowledge")
                    QuestionTypeList(knowledgeTypes)
                    Spacer(modifier = Modifier.height(12.dp))
                    GuideCategoryHeader("To Gather Opinions")
                    QuestionTypeList(opinionTypes)
                }
            }
            item {
                SectionCard(
                    title = "Live Quiz Flow",
                    subtitle = "Local-first delivery keeps sessions resilient"
                ) {
                    GuideParagraph("1. Teacher hosts the session – run the module locally; timing, scoring, and media stay on the teacher device.")
                    GuideParagraph("2. Students join via local network – devices discover the session over LAN or the teacher hotspot, without needing internet.")
                    GuideParagraph("3. Play, score, reflect – leaderboards update live while responses feed instant analytics comparing pre vs post performance.")
                    GuideParagraph("Why local-first matters: sessions stay stable even when the internet drops, student data remains private, and bandwidth needs stay low.")
                }
            }
            item {
                SectionCard(
                    title = "Example Module Flow",
                    subtitle = "Simple Interest and Compound Interest"
                ) {
                    GuideParagraph("Objectives: LO1, LO2, LO3")
                    GuideParagraph("Pre-Test → Lesson Slides → Interactive Quizzes → Post-Test → Report")
                    GuideParagraph("Each stage feeds into the performance report, showing mastery per objective and growth between the pre and post assessments.")
                    GuideParagraph("💡 Tip: Combine pre-test, lesson, and post-test segments with polls or brainstorms to keep engagement high while measuring progress.")
                }
            }
        }
    }
}

private data class QuestionType(
    val name: String,
    val description: String
)

@Composable
private fun GuideParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun GuideStep(
    number: Int,
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$number. $title",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        GuideParagraph(description)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun GuideCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun QuestionTypeList(types: List<QuestionType>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        types.forEach { type ->
            QuestionTypeRow(type)
        }
    }
}

@Composable
private fun QuestionTypeRow(type: QuestionType) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Bullet()
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = type.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = type.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Bullet() {
    Text(
        text = "•",
        modifier = Modifier.padding(top = 2.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
