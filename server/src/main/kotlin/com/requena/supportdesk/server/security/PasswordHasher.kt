package com.requena.supportdesk.server.security

import java.security.MessageDigest
import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

    fun verify(password: String, hash: String): Boolean =
        runCatching { BCrypt.checkpw(password, hash) }.getOrDefault(false)

    // Refresh tokens are high-entropy opaque secrets looked up by exact hash match in SQL,
    // so they need a deterministic digest rather than bcrypt's salted (non-reproducible) hash.
    fun hashToken(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
