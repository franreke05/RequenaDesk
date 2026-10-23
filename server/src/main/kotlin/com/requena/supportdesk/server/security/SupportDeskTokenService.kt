package com.requena.supportdesk.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

data class ServerAuthSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenLifetimeMinutes: Long,
    val refreshTokenLifetimeDays: Long,
)

/** Current state of a user (active, company, role), or null when it can no longer sign in. */
fun interface CurrentIdentityResolver {
    fun current(userId: String): ServerAuthIdentity?
}

class SupportDeskTokenService(
    private val settings: ServerAuthSettings,
    // When set, every verified token is checked against the user's current state, so a
    // removed member or a changed company role takes effect right away instead of when the
    // 8h access token expires. Cached briefly and invalidated on member changes.
    private val currentIdentity: CurrentIdentityResolver? = null,
    private val freshnessWindow: Duration = Duration.ofSeconds(30),
    private val clock: () -> Instant = Instant::now,
) {
    private data class CachedIdentity(val identity: ServerAuthIdentity?, val checkedAt: Instant)

    private val identityCache = ConcurrentHashMap<String, CachedIdentity>()

    /** Forget the cached state of a user (call after changing its role or access). */
    fun invalidate(userId: String) {
        identityCache.remove(userId)
    }

    private fun freshIdentity(userId: String): ServerAuthIdentity? {
        val resolver = currentIdentity ?: return null
        val now = clock()
        val cached = identityCache[userId]
        if (cached != null && Duration.between(cached.checkedAt, now) < freshnessWindow) return cached.identity
        return resolver.current(userId).also { identityCache[userId] = CachedIdentity(it, now) }
    }

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
        identity.organizationId
            ?.takeIf(String::isNotBlank)
            ?.let { builder.withClaim(ORG_ID_CLAIM, it) }
        identity.organizationRole
            ?.takeIf(String::isNotBlank)
            ?.let { builder.withClaim(ORG_ROLE_CLAIM, it) }
        identity.organizationName
            ?.takeIf(String::isNotBlank)
            ?.let { builder.withClaim(ORG_NAME_CLAIM, it) }

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
            organizationId = decoded.getClaim(ORG_ID_CLAIM).asString(),
            organizationRole = decoded.getClaim(ORG_ROLE_CLAIM).asString(),
            organizationName = decoded.getClaim(ORG_NAME_CLAIM).asString(),
        ).takeIf { identity ->
            identity.userId.isNotBlank() &&
                identity.name.isNotBlank() &&
                identity.email.isNotBlank() &&
                identity.role.isNotBlank() &&
                // Tokens minted before V17 carry no company: reject them so the client's
                // automatic refresh (on 401) fetches a new token that does.
                !identity.organizationId.isNullOrBlank()
        }?.let(::withCurrentState)
    } catch (_: JWTVerificationException) {
        null
    }

    /**
     * Applies the user's current company, role and access on top of the token's claims.
     * A deactivated user (or one that moved company) gets null, i.e. 401.
     */
    private fun withCurrentState(identity: ServerAuthIdentity): ServerAuthIdentity? {
        if (currentIdentity == null) return identity
        val current = freshIdentity(identity.userId) ?: return null
        if (current.organizationId != identity.organizationId) return null
        return identity.copy(
            name = current.name,
            role = current.role,
            clientId = current.clientId,
            organizationRole = current.organizationRole,
            organizationName = current.organizationName,
        )
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
        private const val ORG_ID_CLAIM = "orgId"
        private const val ORG_ROLE_CLAIM = "orgRole"
        private const val ORG_NAME_CLAIM = "orgName"

        private val secureRandom = SecureRandom()
    }
}
