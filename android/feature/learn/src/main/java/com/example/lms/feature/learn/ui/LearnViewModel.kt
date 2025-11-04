package com.example.lms.feature.learn.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class LearningPath(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val level: String,
    val description: String,
    val tags: List<String>,
)

data class LearnUiState(
    val enrolled: List<LearningPath>,
    val recommendations: List<LearningPath>,
    val filters: List<String>,
)

@HiltViewModel
class LearnViewModel @Inject constructor() : ViewModel() {
    val uiState = LearnUiState(
        enrolled = listOf(
            LearningPath(
                id = "bio-101",
                title = "Biology: Energy in Cells",
                durationMinutes = 18,
                level = "Intermediate",
                description = "Focus on ATP, mitochondria, and photosynthesis micro-quests.",
                tags = listOf("Biology", "STEM", "9th Grade"),
            ),
        ),
        recommendations = listOf(
            LearningPath(
                id = "hist-201",
                title = "History Sprint: Causes of WWI",
                durationMinutes = 12,
                level = "Adaptive",
                description = "Microlearning recap followed by a pre/post pulse check.",
                tags = listOf("History", "Critical Thinking"),
            ),
            LearningPath(
                id = "math-202",
                title = "Math Lab: Quadratic Sketching",
                durationMinutes = 10,
                level = "Challenging",
                description = "Interactive slider puzzles reinforce vertex and intercept forms.",
                tags = listOf("Mathematics", "Problem Solving"),
            ),
        ),
        filters = listOf("All", "Assigned", "Saved", "Completed"),
    )
}

