package com.example.lms.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.auth.ui.AuthScreen
import org.junit.Rule
import org.junit.Test

class AuthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitle() {
        composeRule.setContent { AuthScreen(title = "Auth") }
        composeRule.onNodeWithText("Auth").assertIsDisplayed()
    }
}

