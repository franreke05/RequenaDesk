package com.requena.supportdesk.server.utils

import io.ktor.server.application.ApplicationCall

const val ADMIN_USER_ID_HEADER = "X-Admin-User-Id"

fun ApplicationCall.adminOwnerId(): String? =
    request.headers[ADMIN_USER_ID_HEADER]
        ?.trim()
        ?.takeIf { it.isNotBlank() }
