package com.classroom.quizmaster.ui.feature.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val todaysTasks: List<String> = emptyList(),
    val continueLearning: List<String> = emptyList(),
    val streak: Int = 0,
    val messages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // TODO: Load data from a repository
        _uiState.value = HomeUiState(
            todaysTasks = listOf("Complete Algebra Pre-Test", "Read Chapter 1"),
            continueLearning = listOf("Introduction to Functions"),
            streak = 5,
            messages = listOf("New assignment posted in 'Algebra 101'")
        )
    }
}
