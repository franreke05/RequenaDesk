package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.LoginRequest
import com.requena.supportdesk.server.domain.model.LogoutRequest
import com.requena.supportdesk.server.domain.model.RefreshSessionRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.sessionJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.errorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(service: SupportDeskService) {
    route("/auth") {
        post("/login") {
            val request = call.receiveOrDefault(LoginRequest())
            if (request.email.isBlank() || request.password.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Email and password are required"))
            }
            val session = service.login(request.email, request.password)
                ?: return@post call.respond(HttpStatusCode.Unauthorized, errorResponse("Invalid credentials"))
            call.respond(successResponse("/auth/login", sessionJson(session)))
        }
        post("/refresh") {
            val request = call.receiveOrDefault(RefreshSessionRequest())
            val session = service.refresh(request)
                ?: return@post call.respond(HttpStatusCode.Unauthorized, errorResponse("Invalid refresh token"))
            call.respond(successResponse("/auth/refresh", sessionJson(session)))
        }
        post("/logout") {
            val request = call.receiveOrDefault(LogoutRequest())
            if (!service.logout(request)) {
                return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Invalid refresh token"))
            }
            call.respond(HttpStatusCode.OK, successResponse("/auth/logout", messageJson("Logout completed.")))
        }
    }
}
