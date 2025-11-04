package com.example.lms.feature.activity

import com.example.lms.feature.activity.ui.ActivityViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class ActivityViewModelTest {
    @Test
    fun () {
        val viewModel = ActivityViewModel()
        assertEquals("Activity", viewModel.title)
    }
}

