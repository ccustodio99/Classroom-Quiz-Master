package com.classroom.quizmaster.util

import java.security.MessageDigest

object Idempotency {
    fun attemptId(uid: String, questionId: String, nonce: String): String {
        val data = "$uid|$questionId|$nonce"
        val digest = MessageDigest.getInstance("SHA-1").digest(data.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
