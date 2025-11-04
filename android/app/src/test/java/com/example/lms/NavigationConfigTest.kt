package com.example.lms

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationConfigTest {
    private val destinations = listOf("home", "auth", "learn", "classroom", "activity", "profile", "live")

    @Test
    fun `start destination is home`() {
        assertEquals("home", destinations.first())
    }

    @Test
    fun `contains live screen`() {
        assertTrue(destinations.contains("live"))
    }
}

