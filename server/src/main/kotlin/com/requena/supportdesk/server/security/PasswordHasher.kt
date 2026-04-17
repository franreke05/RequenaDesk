package com.requena.supportdesk.server.security

import java.security.MessageDigest

object PasswordHasher {
    fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
