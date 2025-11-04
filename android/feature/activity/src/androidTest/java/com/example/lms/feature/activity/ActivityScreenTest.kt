package com.example.lms.feature.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.activity.ui.ActivityScreen
import org.junit.Rule
import org.junit.Test

class ActivityScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitle() {
        composeRule.setContent { ActivityScreen(title = "Activity") }
        composeRule.onNodeWithText("Activity").assertIsDisplayed()
    }
}

