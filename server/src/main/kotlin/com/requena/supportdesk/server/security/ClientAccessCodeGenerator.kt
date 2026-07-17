package com.requena.supportdesk.server.security

import java.security.SecureRandom

/** Generates human-shareable, high-entropy credentials for the client portal. */
object ClientAccessCodeGenerator {
    private const val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generate(): String = buildString {
        append("SBS-")
        repeat(3) { groupIndex ->
            repeat(4) { append(alphabet[random.nextInt(alphabet.length)]) }
            if (groupIndex < 2) append('-')
        }
    }
}
