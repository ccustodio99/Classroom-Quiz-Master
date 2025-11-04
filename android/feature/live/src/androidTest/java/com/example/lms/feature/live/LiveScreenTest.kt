package com.example.lms.feature.live

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.live.ui.LiveScreen
import org.junit.Rule
import org.junit.Test

class LiveScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitle() {
        composeRule.setContent { LiveScreen(title = "Live") }
        composeRule.onNodeWithText("Live").assertIsDisplayed()
    }
}

