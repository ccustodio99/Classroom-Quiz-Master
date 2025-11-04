package com.classroom.quizmaster.ui.feature.activity

import androidx.lifecycle.ViewModel
import com.classroom.quizmaster.domain.model.ActivityTimeline
import com.classroom.quizmaster.domain.model.Badge
import com.classroom.quizmaster.domain.model.Certificate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActivityUiState(
    val timeline: ActivityTimeline? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ActivityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadActivityData()
    }

    private fun loadActivityData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Mock data for demonstration
        val mockBadges = listOf(
            Badge("1", "Fast Learner", "Completed a lesson in under 5 minutes.", ""),
            Badge("2", "Perfect Score", "Got 100% on a quiz.", "")
        )
        val mockCertificates = listOf(
            Certificate("1", "Algebra 101 Completion", "Completed all assignments in Algebra 101.", System.currentTimeMillis(), null)
        )
        val mockTimeline = ActivityTimeline(
            streakDays = 7,
            badges = mockBadges,
            certificates = mockCertificates,
            lastActiveAt = System.currentTimeMillis()
        )

        _uiState.value = ActivityUiState(timeline = mockTimeline)
    }
}
