package com.requena.supportdesk.server.security

import java.security.MessageDigest
import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    fun hashPassword(value: String): String =
        BCrypt.hashpw(value, BCrypt.gensalt(DEFAULT_COST))

    fun verifyPassword(value: String, storedHash: String): Boolean = when {
        isPasswordHash(storedHash) -> BCrypt.checkpw(value, storedHash)
        else -> legacySha256(value) == storedHash
    }

    fun isLegacyPasswordHash(storedHash: String): Boolean = !isPasswordHash(storedHash)

    fun hashToken(value: String): String = legacySha256(value)

    fun hash(value: String): String = hashPassword(value)

    fun legacySha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun isPasswordHash(storedHash: String): Boolean =
        storedHash.startsWith(BCRYPT_PREFIX_2A) ||
            storedHash.startsWith(BCRYPT_PREFIX_2B) ||
            storedHash.startsWith(BCRYPT_PREFIX_2Y)

    private const val DEFAULT_COST = 12
    private const val BCRYPT_PREFIX_2A = "\$2a\$"
    private const val BCRYPT_PREFIX_2B = "\$2b\$"
    private const val BCRYPT_PREFIX_2Y = "\$2y\$"
}
