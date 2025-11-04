package com.example.lms.feature.live

import com.example.lms.feature.live.ui.LiveViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveViewModelTest {
    @Test
    fun () {
        val viewModel = LiveViewModel()
        assertEquals("Live", viewModel.title)
    }
}

