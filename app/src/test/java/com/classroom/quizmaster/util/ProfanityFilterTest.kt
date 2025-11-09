package com.classroom.quizmaster.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfanityFilterTest {

    @AfterEach
    fun tearDown() {
        ProfanityFilter.clearCustomWords()
    }

    @Test
    fun `detects default profanity tokens`() {
        assertTrue(ProfanityFilter.containsProfanity("this is crap"))
    }

    @Test
    fun `sanitize collapses whitespace and replaces profanity`() {
        val result = ProfanityFilter.sanitize("   crap\twizard   ")
        assertFalse(result.contains("crap", ignoreCase = true))
        assertTrue(result.startsWith("Player"))
    }

    @Test
    fun `custom dictionary can be extended`() {
        assertFalse(ProfanityFilter.containsProfanity("wizard"))
        ProfanityFilter.addCustomWords(listOf("wizard"))
        assertTrue(ProfanityFilter.containsProfanity("wizard"))
    }
}
