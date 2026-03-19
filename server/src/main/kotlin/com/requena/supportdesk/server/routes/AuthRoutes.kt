package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.service.SupportDeskPlaceholderService
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.sessionJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.domain.model.LoginRequest
import com.requena.supportdesk.server.utils.errorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(service: SupportDeskPlaceholderService) {
    route("/auth") {
        post("/login") {
            val request = runCatching { call.receive<LoginRequest>() }.getOrDefault(LoginRequest())
            call.respond(successResponse("/auth/login", sessionJson(service.login(request.email))))
        }
        post("/refresh") {
            call.respond(successResponse("/auth/refresh", sessionJson(service.refresh())))
        }
        post("/logout") {
            if (call.request.headers["Authorization"].isNullOrBlank()) {
                call.respond(HttpStatusCode.OK, successResponse("/auth/logout", messageJson(service.logoutMessage())))
            } else {
                call.respond(HttpStatusCode.OK, successResponse("/auth/logout", messageJson(service.logoutMessage())))
            }
        }
    }
}
