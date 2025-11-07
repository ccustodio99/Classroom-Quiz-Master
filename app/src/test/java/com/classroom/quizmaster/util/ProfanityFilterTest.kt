package com.classroom.quizmaster.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfanityFilterTest {

    @Test
    fun `replaces banned words with default`() {
        assertEquals("Player", ProfanityFilter.sanitize("foo"))
    }

    @Test
    fun `returns trimmed nickname when safe`() {
        assertEquals("Friendly", ProfanityFilter.sanitize("  Friendly  "))
    }
}
