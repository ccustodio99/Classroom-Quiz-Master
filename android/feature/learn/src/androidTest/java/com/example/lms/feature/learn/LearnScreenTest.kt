package com.example.lms.feature.learn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.learn.ui.LearnScreen
import org.junit.Rule
import org.junit.Test

class LearnScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitle() {
        composeRule.setContent { LearnScreen(title = "Learn") }
        composeRule.onNodeWithText("Learn").assertIsDisplayed()
    }
}

