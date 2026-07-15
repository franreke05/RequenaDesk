package com.requena.supportdesk.server.plugins

import com.requena.supportdesk.server.domain.model.ServerConflictException
import com.requena.supportdesk.server.domain.model.ServerNotFoundException
import com.requena.supportdesk.server.domain.model.ServerValidationException
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.respondJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages

fun Application.configureMonitoring() {
    install(CallLogging)
    install(StatusPages) {
        exception<ServerNotFoundException> { call, cause ->
            call.respondJson(HttpStatusCode.NotFound, errorResponse(cause.message))
        }
        exception<ServerConflictException> { call, cause ->
            call.respondJson(HttpStatusCode.Conflict, errorResponse(cause.message))
        }
        exception<ServerValidationException> { call, cause ->
            call.respondJson(HttpStatusCode.BadRequest, errorResponse(cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled server error", cause)
            call.respondJson(HttpStatusCode.InternalServerError, errorResponse("Unexpected server error"))
        }
    }
}
