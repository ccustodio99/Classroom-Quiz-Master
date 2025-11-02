package com.classroom.quizmaster.data.util

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.experimental.and

object PasswordHasher {
    private const val SALT_LENGTH_BYTES = 16
    private val secureRandom = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        val hash = digest(salt, password)
        return "${salt.toHex()}:${hash.toHex()}"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].hexToBytes() ?: return false
        val expected = parts[1].lowercase()
        val actual = digest(salt, password).toHex()
        return MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
    }

    private fun digest(salt: ByteArray, password: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(password.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        val v = byte.toInt() and 0xFF
        "%02x".format(v)
    }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return chunked(2).mapNotNull { chunk ->
            chunk.toIntOrNull(16)?.toByte()
        }.toByteArray()
    }
}
