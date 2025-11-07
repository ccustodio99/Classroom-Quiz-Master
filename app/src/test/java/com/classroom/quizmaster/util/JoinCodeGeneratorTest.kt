package com.classroom.quizmaster.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JoinCodeGeneratorTest {

    @Test
    fun `generates code with requested length`() {
        val code = JoinCodeGenerator.generate(8)
        assertEquals(8, code.length)
    }

    @Test
    fun `uses only allowed characters`() {
        val code = JoinCodeGenerator.generate(100)
        assertTrue(code.all { it in "ABCDEFGHJKMNPQRSTUVWXYZ23456789" })
    }
}
