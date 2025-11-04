package com.example.lms.feature.live

import com.example.lms.feature.live.ui.LiveMode
import com.example.lms.feature.live.ui.LiveViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveViewModelTest {
    @Test
    fun preparesLiveSessionState() {
        val state = LiveViewModel().uiState
        assertEquals(LiveMode.HOST, state.mode)
        assertTrue(state.questions.size >= 3)
        assertTrue(state.leaderboard.first().score > 0)
    }
}

