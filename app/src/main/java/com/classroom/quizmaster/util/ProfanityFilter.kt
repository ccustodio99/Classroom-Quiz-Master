package com.classroom.quizmaster.util

object ProfanityFilter {
    private val banned = setOf("badword", "foo", "bar")

    fun sanitize(input: String): String {
        val trimmed = input.trim()
        val replacement = if (trimmed.isBlank()) "Player" else trimmed
        return if (banned.any { word -> replacement.contains(word, ignoreCase = true) }) {
            "Player"
        } else {
            replacement
        }
    }
}
