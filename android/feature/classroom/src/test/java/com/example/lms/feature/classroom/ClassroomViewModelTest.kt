package com.example.lms.feature.classroom

import com.example.lms.feature.classroom.ui.ClassroomViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassroomViewModelTest {
    @Test
    fun () {
        val viewModel = ClassroomViewModel()
        assertEquals("Classroom", viewModel.title)
    }
}

