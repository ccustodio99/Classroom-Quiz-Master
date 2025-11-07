package com.classroom.quizmaster

import com.classroom.quizmaster.util.ProfanityFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfanityFilterTest {
    @Test
    fun `replaces banned words`() {
        assertEquals("Player", ProfanityFilter.sanitize("badword kid"))
    }

    @Test
    fun `keeps clean name`() {
        assertEquals("Alex", ProfanityFilter.sanitize("Alex"))
    }
}
