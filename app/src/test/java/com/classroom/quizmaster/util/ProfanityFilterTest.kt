package com.classroom.quizmaster.util

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test

class ProfanityFilterTest {

    @After
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
