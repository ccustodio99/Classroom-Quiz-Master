package com.example.lms.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ModelsTest {
    @Test
    fun `lms result success exposes value`() {
        val success = LmsResult.Success(5)
        assertEquals(5, success.value)
    }
}

