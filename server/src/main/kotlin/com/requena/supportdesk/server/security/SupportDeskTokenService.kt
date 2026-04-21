package com.requena.supportdesk.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date

data class ServerAuthSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenLifetimeMinutes: Long,
    val refreshTokenLifetimeDays: Long,
)

class SupportDeskTokenService(
    private val settings: ServerAuthSettings,
) {
    private val algorithm = Algorithm.HMAC256(settings.secret)
    private val verifier = JWT.require(algorithm)
        .withIssuer(settings.issuer)
        .withAudience(settings.audience)
        .withClaim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
        .acceptLeeway(5)
        .build()

    fun createAccessToken(
        identity: ServerAuthIdentity,
        now: Instant = Instant.now(),
    ): String {
        val builder = JWT.create()
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .withSubject(identity.userId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(settings.accessTokenLifetimeMinutes, ChronoUnit.MINUTES)))
            .withClaim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .withClaim(NAME_CLAIM, identity.name)
            .withClaim(EMAIL_CLAIM, identity.email)
            .withClaim(ROLE_CLAIM, identity.role)

        identity.clientId
            ?.takeIf(String::isNotBlank)
            ?.let { builder.withClaim(CLIENT_ID_CLAIM, it) }

        return builder.sign(algorithm)
    }

    fun createRefreshToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }

    fun verifyAccessToken(token: String): ServerAuthIdentity? = try {
        val decoded = verifier.verify(token)
        ServerAuthIdentity(
            userId = decoded.subject.orEmpty(),
            name = decoded.getClaim(NAME_CLAIM).asString().orEmpty(),
            email = decoded.getClaim(EMAIL_CLAIM).asString().orEmpty(),
            role = decoded.getClaim(ROLE_CLAIM).asString().orEmpty(),
            clientId = decoded.getClaim(CLIENT_ID_CLAIM).asString(),
        ).takeIf { identity ->
            identity.userId.isNotBlank() &&
                identity.name.isNotBlank() &&
                identity.email.isNotBlank() &&
                identity.role.isNotBlank()
        }
    } catch (_: JWTVerificationException) {
        null
    }

    fun refreshTokenExpiresAt(now: Instant = Instant.now()): Instant =
        now.plus(settings.refreshTokenLifetimeDays, ChronoUnit.DAYS)

    companion object {
        private const val TOKEN_TYPE_CLAIM = "type"
        private const val ACCESS_TOKEN_TYPE = "access"
        private const val NAME_CLAIM = "name"
        private const val EMAIL_CLAIM = "email"
        private const val ROLE_CLAIM = "role"
        private const val CLIENT_ID_CLAIM = "clientId"

        private val secureRandom = SecureRandom()
    }
}
