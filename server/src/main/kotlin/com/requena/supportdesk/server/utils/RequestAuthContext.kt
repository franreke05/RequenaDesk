package com.requena.supportdesk.server.utils

import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.domain.service.OrganizationRoles
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod

private const val bearerScheme = "Bearer"
private const val bearerRealm = "OryKai software"
private val readOnlyMethods = setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options)

suspend fun ApplicationCall.requireAuthenticatedIdentity(
    tokenService: SupportDeskTokenService,
    allowQueryToken: Boolean = false,
): ServerAuthIdentity? {
    val authorizationHeader = request.headers[HttpHeaders.Authorization].orEmpty()
    val authHeader = runCatching { parseAuthorizationHeader(authorizationHeader) }.getOrNull() as? HttpAuthHeader.Single
    val token = authHeader
        ?.takeIf { it.authScheme.equals(bearerScheme, ignoreCase = true) }
        ?.blob
        ?.trim()
        ?.takeIf(String::isNotBlank)

    val resolvedToken = token ?: request.queryParameters["access_token"]
        ?.takeIf { allowQueryToken }
        ?.trim()
        ?.takeIf(String::isNotBlank)
    val identity = resolvedToken?.let(tokenService::verifyAccessToken)
    if (identity != null) {
        // Gestoría (ACCOUNTANT) sees everything but changes nothing. Every authenticated route
        // goes through here, so blocking non-read methods here covers all of them at once.
        if (OrganizationRoles.isReadOnly(identity.organizationRole) && request.httpMethod !in readOnlyMethods) {
            respondJson(
                status = HttpStatusCode.Forbidden,
                body = errorResponse("Your company role is read-only"),
            )
            return null
        }
        return identity
    }

    response.headers.append(HttpHeaders.WWWAuthenticate, """Bearer realm="$bearerRealm"""")
    respondJson(
        status = HttpStatusCode.Unauthorized,
        body = errorResponse("Valid bearer token is required"),
    )
    return null
}

suspend fun ApplicationCall.requireAdminIdentity(
    tokenService: SupportDeskTokenService,
): ServerAuthIdentity? {
    val identity = requireAuthenticatedIdentity(tokenService) ?: return null
    if (identity.role == "ADMIN") {
        return identity
    }

    respondJson(
        status = HttpStatusCode.Forbidden,
        body = errorResponse("Admin role is required"),
    )
    return null
}

val ServerAuthIdentity.isAdmin: Boolean
    get() = role == "ADMIN"

/**
 * The caller's company (tenant). Every data query is scoped by it. Verified access tokens
 * always carry it (SupportDeskTokenService rejects tokens without one).
 */
val ServerAuthIdentity.orgId: String
    get() = organizationId?.takeIf(String::isNotBlank)
        ?: error("Access token without organization reached a route")

fun SupportDeskService.visibleClientIdsFor(identity: ServerAuthIdentity): Set<String> =
    if (identity.isAdmin) {
        clients(identity.orgId).mapTo(mutableSetOf()) { it.id }
    } else {
        setOfNotNull(identity.clientId)
    }
