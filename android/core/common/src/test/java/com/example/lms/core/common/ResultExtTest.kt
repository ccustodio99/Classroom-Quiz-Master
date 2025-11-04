package com.example.lms.core.common

import com.example.lms.core.model.LmsResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultExtTest {
    @Test
    fun `map transforms success`() {
        val result: LmsResult<Int> = LmsResult.Success(2)
        val mapped = result.map { it * 2 }
        assertEquals(LmsResult.Success(4), mapped)
    }
}

