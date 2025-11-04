package com.example.lms.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.home.ui.HomeScreen
import com.example.lms.feature.home.ui.HomeTask
import com.example.lms.feature.home.ui.HomeUiState
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersDashboardHighlights() {
        val state = HomeUiState(
            learnerName = "Test",
            streakDays = 2,
            minutesToGoal = 10,
            currentModule = "Chemistry Warmup",
            todayTasks = listOf(HomeTask("Warmup", "Due soon", "Quiz")),
            messages = listOf("Host shared a live code"),
        )
        composeRule.setContent {
            HomeScreen(
                state = state,
                onContinueLearning = {},
                onOpenClassroom = {},
                onOpenProfile = {},
            )
        }
        composeRule.onNodeWithText("Welcome back, Test").assertIsDisplayed()
        composeRule.onNodeWithText("Warmup").assertIsDisplayed()
    }
}

