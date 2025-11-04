package com.example.lms.feature.home

import com.example.lms.feature.home.ui.HomeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeViewModelTest {
    @Test
    fun exposesDefaultHomeDashboard() {
        val state = HomeViewModel().uiState
        assertEquals("Alex", state.learnerName)
        assertTrue(state.todayTasks.isNotEmpty())
        assertTrue(state.messages.any { it.contains("Live", ignoreCase = true) })
    }
}

