package com.classroom.quizmaster

import com.classroom.quizmaster.util.NicknamePolicy
import java.security.MessageDigest
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NicknamePolicyTest {

    @Test
    fun sanitizeClampsLengthAndAddsStableSuffix() {
        val salt = "salty-seed"
        val expectedSuffix = MessageDigest.getInstance("SHA-1")
            .digest(salt.toByteArray())
            .take(2)
            .joinToString("") { "%02x".format(Locale.US, it) }
            .uppercase(Locale.US)
        val result = NicknamePolicy.sanitize("   badword!!player-name  ", salt)
        assertEquals("Player-$expectedSuffix", result)
        require(result.length <= 16)
    }

    @Test
    fun validationDetectsTooShortNickname() {
        assertNull(NicknamePolicy.validationError("PlayerOne"))
        val error = NicknamePolicy.validationError("a")
        assertEquals("Nickname must be at least 2 characters", error)
    }
}
