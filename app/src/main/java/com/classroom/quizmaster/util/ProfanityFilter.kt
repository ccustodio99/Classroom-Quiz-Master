package com.classroom.quizmaster.util

import java.util.Collections
import java.util.Locale

object ProfanityFilter {
    private val sanitizeRegex = Regex("[^a-z0-9 ]")
    private val whitespaceRegex = Regex("\\s+")
    private val defaultDictionary = setOf(
        "arse",
        "ass",
        "bastard",
        "bitch",
        "bloody",
        "bollocks",
        "crap",
        "cum",
        "damn",
        "dick",
        "fuck",
        "hell",
        "piss",
        "poop",
        "porn",
        "sex",
        "shit",
        "slut",
        "suck",
        "whore"
    )
    private val customDictionary: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    fun sanitize(input: String, fallback: String = "Player"): String {
        val candidate = input.trim().ifBlank { fallback }
        return if (containsProfanity(candidate)) fallback else collapseWhitespace(candidate)
    }

    fun containsProfanity(input: String): Boolean {
        val combined = if (customDictionary.isEmpty()) {
            defaultDictionary
        } else {
            defaultDictionary + customDictionary
        }
        return tokens(input).any { it in combined }
    }

    fun addCustomWords(words: Collection<String>) {
        if (words.isEmpty()) return
        customDictionary += words.map { it.lowercase(Locale.US) }
    }

    fun clearCustomWords() {
        customDictionary.clear()
    }

    private fun collapseWhitespace(value: String): String =
        value.replace(whitespaceRegex, " ")

    private fun tokens(value: String): Sequence<String> = sequence {
        val normalized = value.lowercase(Locale.US).replace(sanitizeRegex, " ")
        normalized.split(whitespaceRegex)
            .asSequence()
            .filter { it.isNotBlank() }
            .forEach { yield(it) }
    }
}
