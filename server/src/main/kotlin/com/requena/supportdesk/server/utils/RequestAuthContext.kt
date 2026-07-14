package com.requena.supportdesk.server.utils

import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.server.application.ApplicationCall

private const val bearerScheme = "Bearer"
private const val bearerRealm = "OryKai software"

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

fun SupportDeskService.ownerAdminIdFor(identity: ServerAuthIdentity): String? =
    if (identity.isAdmin) {
        identity.userId
    } else {
        identity.clientId
            ?.let { clientId -> clients().firstOrNull { it.id == clientId } }
            ?.ownerAdminId
    }

fun SupportDeskService.visibleClientIdsFor(identity: ServerAuthIdentity): Set<String> =
    if (identity.isAdmin) {
        clients(identity.userId).mapTo(mutableSetOf()) { it.id }
    } else {
        setOfNotNull(identity.clientId)
    }
