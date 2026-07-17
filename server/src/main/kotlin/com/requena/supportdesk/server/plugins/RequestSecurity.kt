package com.requena.supportdesk.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

val AuthRateLimit = RateLimitName("auth")
val SensitiveOperationRateLimit = RateLimitName("sensitive-operation")

fun Application.configureRequestSecurity() {
    // Ktor listens only on localhost; Caddy is the trusted public reverse proxy.
    install(XForwardedHeaders)
    install(RateLimit) {
        register(AuthRateLimit) {
            rateLimiter(limit = 8, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(SensitiveOperationRateLimit) {
            rateLimiter(limit = 5, refillPeriod = 10.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
}
