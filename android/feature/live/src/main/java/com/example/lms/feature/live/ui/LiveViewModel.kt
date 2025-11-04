package com.example.lms.feature.live.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class LiveMode { HOST, PARTICIPANT }

data class LiveQuestionUi(
    val title: String,
    val prompt: String,
    val type: String,
    val options: List<String> = emptyList(),
    val sliderRange: IntRange? = null,
)

data class LiveUiState(
    val mode: LiveMode,
    val sessionCode: String,
    val connectionStatus: String,
    val currentQuestionIndex: Int,
    val questions: List<LiveQuestionUi>,
    val leaderboard: List<com.example.lms.core.model.LeaderboardEntry>,
)

@HiltViewModel
class LiveViewModel @Inject constructor() : ViewModel() {
    val uiState = LiveUiState(
        mode = LiveMode.HOST,
        sessionCode = "729 184",
        connectionStatus = "Broadcasting on _lms._udp",
        currentQuestionIndex = 0,
        questions = listOf(
            LiveQuestionUi(
                title = "Question 1",
                prompt = "Which structure captures light energy during photosynthesis?",
                type = "MCQ",
                options = listOf("Mitochondria", "Chloroplast", "Nucleus", "Ribosome"),
            ),
            LiveQuestionUi(
                title = "Question 2",
                prompt = "True or false: The Calvin cycle occurs in the thylakoid lumen.",
                type = "TF",
                options = listOf("True", "False"),
            ),
            LiveQuestionUi(
                title = "Question 3",
                prompt = "Drag the phases of photosynthesis into order.",
                type = "Puzzle",
                options = listOf("Light capture", "Electron transport", "ATP synthase", "Carbon fixation"),
            ),
            LiveQuestionUi(
                title = "Question 4",
                prompt = "Estimate the pH change during the light reactions.",
                type = "Slider",
                sliderRange = 0..14,
            ),
        ),
        leaderboard = listOf(
            com.example.lms.core.model.LeaderboardEntry("alex", "Alex", 920.0, 3, 850),
            com.example.lms.core.model.LeaderboardEntry("jordan", "Jordan", 880.0, 2, 1020),
            com.example.lms.core.model.LeaderboardEntry("priya", "Priya", 870.0, 4, 910),
        ),
    )
}

