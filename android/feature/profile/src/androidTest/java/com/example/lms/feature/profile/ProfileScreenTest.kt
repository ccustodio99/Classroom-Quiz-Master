package com.example.lms.feature.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.profile.ui.ProfileScreen
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitle() {
        composeRule.setContent { ProfileScreen(title = "Profile") }
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
    }
}

