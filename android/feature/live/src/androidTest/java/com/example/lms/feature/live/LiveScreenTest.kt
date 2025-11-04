package com.example.lms.feature.live

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.live.ui.LiveMode
import com.example.lms.feature.live.ui.LiveQuestionUi
import com.example.lms.feature.live.ui.LiveScreen
import com.example.lms.feature.live.ui.LiveUiState
import org.junit.Rule
import org.junit.Test

class LiveScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysQuestionAndLeaderboard() {
        val state = LiveUiState(
            mode = LiveMode.HOST,
            sessionCode = "123 456",
            connectionStatus = "Broadcasting",
            currentQuestionIndex = 0,
            questions = listOf(
                LiveQuestionUi("Q1", "Prompt", "MCQ", listOf("A", "B")),
            ),
            leaderboard = emptyList(),
        )
        composeRule.setContent {
            LiveScreen(
                state = state,
                onExit = {},
            )
        }
        composeRule.onNodeWithText("Live session").assertIsDisplayed()
        composeRule.onNodeWithText("Prompt").assertIsDisplayed()
    }
}

