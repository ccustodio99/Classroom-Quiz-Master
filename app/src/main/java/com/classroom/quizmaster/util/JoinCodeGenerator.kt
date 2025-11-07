package com.classroom.quizmaster.util

import kotlin.random.Random

object JoinCodeGenerator {
    private const val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun generate(length: Int = 6): String =
        buildString {
            repeat(length) {
                append(alphabet.random())
            }
        }
}
