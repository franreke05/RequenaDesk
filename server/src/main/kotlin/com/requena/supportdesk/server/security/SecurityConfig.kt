package com.requena.supportdesk.server.security

object SecurityConfig {
    const val authSecretKey = "SUPPORTDESK_AUTH_SECRET"
    const val legacyJwtSecretKey = "SUPPORTDESK_JWT_SECRET"
    const val authIssuerKey = "SUPPORTDESK_AUTH_ISSUER"
    const val authAudienceKey = "SUPPORTDESK_AUTH_AUDIENCE"
    const val accessTokenLifetimeMinutesKey = "SUPPORTDESK_ACCESS_TOKEN_LIFETIME_MINUTES"
    const val refreshTokenLifetimeDaysKey = "SUPPORTDESK_REFRESH_TOKEN_LIFETIME_DAYS"

    const val defaultIssuer = "requenadesk-server"
    const val defaultAudience = "requenadesk-clients"
    const val defaultAccessTokenLifetimeMinutes = 480L
    const val defaultRefreshTokenLifetimeDays = 30L
    const val minimumSecretLength = 32
}
