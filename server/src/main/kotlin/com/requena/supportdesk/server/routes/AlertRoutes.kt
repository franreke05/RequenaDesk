package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.alertJson
import com.requena.supportdesk.server.utils.alertsJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.alertRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/alerts") {
        get {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceIn(0, 10_000) ?: 0
            call.respondJson(
                body = successResponse(
                    "/alerts",
                    alertsJson(service.alerts(identity.userId, limit, offset)),
                ),
            )
        }
        post("/{id}/read") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing alert id"))
            val alert = service.readAlert(id, identity.userId)
                ?: return@post call.respondJson(HttpStatusCode.NotFound, errorResponse("Alert not found"))
            call.respondJson(body = successResponse("/alerts/$id/read", alertJson(alert)))
        }
    }
}
