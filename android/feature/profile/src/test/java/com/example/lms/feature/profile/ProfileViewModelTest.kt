package com.example.lms.feature.profile

import com.example.lms.feature.profile.ui.ProfileViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileViewModelTest {
    @Test
    fun () {
        val viewModel = ProfileViewModel()
        assertEquals("Profile", viewModel.title)
    }
}

