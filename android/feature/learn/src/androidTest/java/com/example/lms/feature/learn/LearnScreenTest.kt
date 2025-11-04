package com.example.lms.feature.learn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.learn.ui.LearnScreen
import com.example.lms.feature.learn.ui.LearnUiState
import com.example.lms.feature.learn.ui.LearningPath
import org.junit.Rule
import org.junit.Test

class LearnScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersCatalogSections() {
        val state = LearnUiState(
            enrolled = listOf(
                LearningPath("id", "Test Course", 5, "Intro", "Sample", emptyList()),
            ),
            recommendations = emptyList(),
            filters = listOf("All"),
        )
        composeRule.setContent {
            LearnScreen(
                state = state,
                onSelectClass = {},
                onStartSearch = {},
            )
        }
        composeRule.onNodeWithText("Catalog").assertIsDisplayed()
        composeRule.onNodeWithText("Test Course").assertIsDisplayed()
    }
}

