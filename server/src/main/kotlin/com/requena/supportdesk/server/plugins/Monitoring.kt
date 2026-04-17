package com.requena.supportdesk.server.plugins

import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.utils.errorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureMonitoring() {
    install(CallLogging)
    install(StatusPages) {
        exception<ServerNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, errorResponse(cause.message ?: "Resource not found"))
        }
        exception<ServerConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, errorResponse(cause.message ?: "Conflict"))
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, errorResponse(cause.message ?: "Unexpected error"))
        }
    }
}
