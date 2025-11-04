package com.example.lms.feature.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.profile.ui.OfflineDownload
import com.example.lms.feature.profile.ui.PreferenceItem
import com.example.lms.feature.profile.ui.ProfileScreen
import com.example.lms.feature.profile.ui.ProfileUiState
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsAccountDetails() {
        val state = ProfileUiState(
            displayName = "Test User",
            email = "test@example.com",
            role = "Learner",
            org = "Demo Org",
            downloads = listOf(OfflineDownload("Module", 12)),
            preferences = listOf(PreferenceItem("Pref", "Value")),
        )
        composeRule.setContent {
            ProfileScreen(
                state = state,
                onSignOut = {},
                onManageDownloads = {},
            )
        }
        composeRule.onNodeWithText("Test User • Learner").assertIsDisplayed()
        composeRule.onNodeWithText("Module").assertIsDisplayed()
    }
}

