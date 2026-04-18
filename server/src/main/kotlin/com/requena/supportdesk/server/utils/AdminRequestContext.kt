package com.requena.supportdesk.server.utils

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

const val ADMIN_USER_ID_HEADER = "X-Admin-User-Id"

fun ApplicationCall.adminOwnerId(): String? =
    request.headers[ADMIN_USER_ID_HEADER]
        ?.trim()
        ?.takeIf { it.isNotBlank() }

suspend fun ApplicationCall.requireAdminOwnerId(): String? {
    val ownerAdminId = adminOwnerId()
    if (ownerAdminId != null) {
        return ownerAdminId
    }
    respondJson(
        status = HttpStatusCode.BadRequest,
        body = errorResponse("$ADMIN_USER_ID_HEADER header is required"),
    )
    return null
}
