package com.example.lms.feature.classroom

import com.example.lms.feature.classroom.ui.ClassroomViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassroomViewModelTest {
    @Test
    fun containsStreamAndRoster() {
        val state = ClassroomViewModel().uiState
        assertEquals("Biology Lab", state.className)
        assertTrue(state.stream.isNotEmpty())
        assertTrue(state.learners.any { it.contains("Alex") })
    }
}

