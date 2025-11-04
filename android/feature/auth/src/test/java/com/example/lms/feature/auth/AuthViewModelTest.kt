package com.example.lms.feature.auth

import com.example.lms.feature.auth.ui.AuthViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthViewModelTest {
    @Test
    fun () {
        val viewModel = AuthViewModel()
        assertEquals("Auth", viewModel.title)
    }
}

