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
        QuestionType(
            name = "Quiz",
            description = "Classic 2–4 option multiple-choice items. Enable one or multiple correct answers."
        ),
        QuestionType(
            name = "True / False",
            description = "Fast checks for factual understanding with two possible answers."
        ),
        QuestionType(
            name = "Type Answer",
            description = "Learners type a short response (≤20 characters) instead of picking from options."
        ),
        QuestionType(
            name = "Puzzle",
            description = "Drag and drop blocks into the correct order to build a sequence or process."
        ),
        QuestionType(
            name = "Slider",
            description = "Move a pin to the exact number on a scale (e.g., year founded or value on a number line)."
        )
    )
    val opinionTypes = listOf(
        QuestionType(
            name = "Poll",
            description = "Gauge opinions quickly with multiple-choice options — no points awarded."
        ),
        QuestionType(
            name = "Word Cloud",
            description = "Players submit short words; the most popular answers appear larger on-screen."
        ),
        QuestionType(
            name = "Open-Ended",
            description = "Collect longer reflections or feedback with paragraph-style responses."
        ),
        QuestionType(
            name = "Brainstorm",
            description = "Gather ideas collaboratively, then vote on favorites to surface top contributions."
        )
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
                    title = "Classroom & Module Builder",
                    subtitle = "Capture everything a teacher needs before delivery"
                ) {
                    GuideParagraph("Create a classroom or subject profile with:")
                    GuideBulletList(
                        listOf(
                            "Classroom or subject name",
                            "Details or reminders for the group",
                            "Grade level (pick from presets or type a custom number)",
                            "Section (optional label for strands, tracks, or advisory groups)"
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GuideParagraph("Inside each module, add topics or lessons that include:")
                    GuideBulletList(
                        listOf(
                            "Topic name, learning objectives, and detailed notes",
                            "Learning materials — upload links to documents, presentations, spreadsheets, media files, or other resources",
                            "Pre-tests with multiple-choice questions and answer keys",
                            "Post-tests that support multiple-choice, true/false, numeric, and other formats",
                            "Interactive assessments that mirror Kahoot-style live quizzes"
                        )
                    )
                }
            }
            item {
                SectionCard(
                    title = "The Host's Role",
                    subtitle = "Step-by-step guide for teachers or presenters"
                ) {
                    GuideStep(
                        number = 1,
                        title = "Create or find a kahoot",
                        description = "Build your own quiz on Classroom Quiz Master or reuse a shared public module from the item bank."
                    )
                    GuideStep(
                        number = 2,
                        title = "Launch the live game",
                        description = "Start a live session to generate a unique Game PIN (e.g., 123 4567). Share it on the projector, TV, or video call screen."
                    )
                    GuideStep(
                        number = 3,
                        title = "Share the main screen",
                        description = "Display questions, answer choices, and the evolving leaderboard so everyone follows the same pacing."
                    )
                }
            }
            item {
                SectionCard(
                    title = "The Players' Role",
                    subtitle = "How students join and participate"
                ) {
                    GuideStep(
                        number = 1,
                        title = "Join the game",
                        description = "No account needed. Open kahoot.it or the Classroom Quiz Master join screen on any device."
                    )
                    GuideStep(
                        number = 2,
                        title = "Enter the PIN and nickname",
                        description = "Type the Game PIN from the host’s display, choose a nickname, and wait for the quiz to start."
                    )
                    GuideStep(
                        number = 3,
                        title = "Play the game",
                        description = "Questions appear on the shared screen. Personal devices show colored answer shapes — tap the matching shape as fast and accurately as possible."
                    )
                    GuideBulletList(
                        listOf(
                            "Points reward correctness and speed (no speed bonuses during diagnostics).",
                            "Leaderboards update after each question, highlighting the top five players.",
                            "A final podium celebrates the top three performers."
                        )
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
private fun GuideBulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { text ->
            GuideBullet(text)
        }
    }
}

@Composable
private fun GuideBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Bullet()
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
