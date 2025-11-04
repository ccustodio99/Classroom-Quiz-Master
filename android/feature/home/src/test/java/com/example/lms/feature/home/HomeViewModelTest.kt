package com.example.lms.feature.home

import com.example.lms.feature.home.ui.HomeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelTest {
    @Test
    fun () {
        val viewModel = HomeViewModel()
        assertEquals("Home", viewModel.title)
    }
}

