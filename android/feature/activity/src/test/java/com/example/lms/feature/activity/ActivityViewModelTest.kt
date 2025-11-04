package com.example.lms.feature.activity

import com.example.lms.feature.activity.ui.ActivityViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityViewModelTest {
    @Test
    fun providesProgressSnapshots() {
        val state = ActivityViewModel().uiState
        assertEquals(4, state.streak)
        assertTrue(state.progress.any { it.title.contains("Biology") })
        assertTrue(state.badges.isNotEmpty())
    }
}

