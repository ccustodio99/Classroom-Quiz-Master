package com.classroom.quizmaster.util

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JoinCodeGeneratorTest {

    @Test
    fun `generate produces valid uppercase code`() {
        val code = JoinCodeGenerator.generate(length = 6, random = Random(1))
        assertEquals(6, code.length)
        assertTrue(code.all { it.isUpperCase() || it.isDigit() })
        assertTrue(JoinCodeGenerator.isValid(code))
    }

    @Test
    fun `generator replaces banned substrings when necessary`() {
        val random = object : Random() {
            private val values = intArrayOf(1, 0, 3, 1, 0, 3)
            private var index = 0
            override fun nextInt(bound: Int): Int = values[index++ % values.size] % bound
            override fun nextBits(bitCount: Int): Int {
                val value = values[index++ % values.size]
                return if (bitCount >= Int.SIZE_BITS) value else value and ((1 shl bitCount) - 1)
            }
        }
        val code = JoinCodeGenerator.generate(length = 6, random = random)
        assertEquals(6, code.length)
        assertTrue("BAD" !in code)
    }

    @Test
    fun `parseOrNull rejects invalid characters`() {
        assertNull(JoinCodeGenerator.parseOrNull("abc$%"))
    }

    @Test
    fun `normalize strips invalid characters`() {
        val normalized = JoinCodeGenerator.normalize(" ab-12 CD ")
        assertEquals("AB12CD", normalized)
    }

    @Test
    fun `requireValid throws for short code`() {
        val exception = kotlin.runCatching { JoinCodeGenerator.requireValid("ABC") }.exceptionOrNull()
        assertNotNull(exception)
    }
}
