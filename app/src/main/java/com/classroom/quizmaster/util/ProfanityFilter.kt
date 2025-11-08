package com.classroom.quizmaster.util

import java.util.Locale

object ProfanityFilter {
    private val bannedTokens = setOf(
        "badword",
        "foo",
        "bar",
        "spam",
        "egregious",
        "darn",
        "heck"
    )

    private val sanitizeRegex = Regex("[^a-z0-9 ]")

    fun sanitize(input: String, fallback: String = "Player"): String {
        val candidate = input.trim().ifBlank { fallback }
        return if (containsProfanity(candidate)) fallback else candidate
    }

    fun containsProfanity(input: String): Boolean =
        tokens(input).any { it in bannedTokens }

    private fun tokens(value: String): Sequence<String> =
        value.lowercase(Locale.US)
            .replace(sanitizeRegex, " ")
            .split(Regex("\\s+"))
            .asSequence()
            .filter { it.isNotBlank() }
}
