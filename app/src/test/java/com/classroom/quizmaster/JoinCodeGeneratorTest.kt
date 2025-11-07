package com.classroom.quizmaster

import com.classroom.quizmaster.util.JoinCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoinCodeGeneratorTest {
    @Test
    fun `generates uppercase code`() {
        val code = JoinCodeGenerator.generate()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isLetterOrDigit() && !setOf('O', 'I', '0', '1').contains(it) })
    }
}
