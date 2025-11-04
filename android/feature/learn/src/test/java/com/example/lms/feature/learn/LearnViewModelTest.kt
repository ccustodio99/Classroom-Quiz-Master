package com.example.lms.feature.learn

import com.example.lms.feature.learn.ui.LearnViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class LearnViewModelTest {
    @Test
    fun () {
        val viewModel = LearnViewModel()
        assertEquals("Learn", viewModel.title)
    }
}

