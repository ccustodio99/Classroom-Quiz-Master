package com.classroom.quizmaster.util

import kotlin.math.ceil

object ScoreCalculator {
    fun score(correct: Boolean, timeLeftMillis: Long, timeLimitMillis: Long): Int {
        if (!correct) return 0
        if (timeLimitMillis <= 0L) return 400
        val ratio = timeLeftMillis.toDouble() / timeLimitMillis.toDouble()
        return ceil(400 + 600 * ratio).toInt().coerceAtMost(1000)
    }
}
