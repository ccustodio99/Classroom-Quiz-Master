package com.example.lms.feature.home.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class HomeTask(
    val title: String,
    val due: String,
    val type: String,
)

data class HomeUiState(
    val learnerName: String,
    val streakDays: Int,
    val minutesToGoal: Int,
    val currentModule: String,
    val todayTasks: List<HomeTask>,
    val messages: List<String>,
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    val uiState = HomeUiState(
        learnerName = "Alex",
        streakDays = 4,
        minutesToGoal = 12,
        currentModule = "Microlearning: Photosynthesis Basics",
        todayTasks = listOf(
            HomeTask("Finish Pretest: Chloroplast Tour", "Due in 2h", "Pretest"),
            HomeTask("Watch: Light Reactions (5 min)", "Due today", "Video"),
            HomeTask("Quiz: Energy Transfer", "Due tomorrow", "Quiz"),
        ),
        messages = listOf(
            "Ms. Rivera pinned a lab safety checklist for today's class",
            "Live challenge starts at 2:05 PM—bring your device",
        ),
    )
}

