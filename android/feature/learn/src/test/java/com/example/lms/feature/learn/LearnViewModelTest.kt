package com.example.lms.feature.learn

import com.example.lms.feature.learn.ui.LearnViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LearnViewModelTest {
    @Test
    fun providesEnrolledCatalog() {
        val state = LearnViewModel().uiState
        assertTrue(state.enrolled.isNotEmpty())
        assertEquals("Biology: Energy in Cells", state.enrolled.first().title)
        assertTrue(state.filters.contains("Assigned"))
    }
}

