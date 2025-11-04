package com.example.lms.feature.activity.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ProgressSnapshot(
    val title: String,
    val completionPercent: Int,
    val masteryDelta: Int,
)

data class Badge(
    val name: String,
    val description: String,
)

data class Certificate(
    val title: String,
    val issuedOn: String,
)

data class ActivityUiState(
    val streak: Int,
    val masteryGain: Double,
    val progress: List<ProgressSnapshot>,
    val badges: List<Badge>,
    val certificates: List<Certificate>,
)

@HiltViewModel
class ActivityViewModel @Inject constructor() : ViewModel() {
    val uiState = ActivityUiState(
        streak = 4,
        masteryGain = 0.18,
        progress = listOf(
            ProgressSnapshot("Biology Lab", 76, +12),
            ProgressSnapshot("World History", 52, +6),
        ),
        badges = listOf(
            Badge("Lab Champion", "Completed three consecutive labs with full credit"),
            Badge("Lightning Learner", "Answered live questions under 2s average latency"),
        ),
        certificates = listOf(
            Certificate("Microlearning Sprint: Cells", "Issued May 2"),
        ),
    )
}

