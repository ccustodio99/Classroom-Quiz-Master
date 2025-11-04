package com.example.lms.feature.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.activity.ui.ActivityScreen
import com.example.lms.feature.activity.ui.ActivityUiState
import com.example.lms.feature.activity.ui.Badge
import com.example.lms.feature.activity.ui.Certificate
import com.example.lms.feature.activity.ui.ProgressSnapshot
import org.junit.Rule
import org.junit.Test

class ActivityScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsProgressAndBadges() {
        val state = ActivityUiState(
            streak = 3,
            masteryGain = 0.2,
            progress = listOf(ProgressSnapshot("Course", 80, 5)),
            badges = listOf(Badge("Badge", "Desc")),
            certificates = listOf(Certificate("Cert", "Issued")),
        )
        composeRule.setContent {
            ActivityScreen(
                state = state,
                onOpenCertificates = {},
            )
        }
        composeRule.onNodeWithText("Progress").assertIsDisplayed()
        composeRule.onNodeWithText("Badge").assertIsDisplayed()
    }
}

