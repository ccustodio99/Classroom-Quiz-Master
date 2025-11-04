package com.example.lms.feature.profile

import com.example.lms.feature.profile.ui.ProfileViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileViewModelTest {
    @Test
    fun exposesProfileDetails() {
        val state = ProfileViewModel().uiState
        assertEquals("Alex Kim", state.displayName)
        assertTrue(state.downloads.isNotEmpty())
        assertTrue(state.preferences.any { it.label.contains("Reminder") })
    }
}

