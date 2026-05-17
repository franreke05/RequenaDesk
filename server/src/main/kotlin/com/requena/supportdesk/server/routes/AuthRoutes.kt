package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.ClientAccessCodeClaimRequest
import com.requena.supportdesk.server.domain.model.LoginRequest
import com.requena.supportdesk.server.domain.model.LogoutRequest
import com.requena.supportdesk.server.domain.model.RefreshSessionRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.sessionJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.request.receiveNullable

fun Route.authRoutes(service: SupportDeskService) {
    route("/auth") {
        post("/login") {
            val request = call.receiveLoginRequest()
            if (request.email.isBlank() || request.password.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Email and password are required"))
            }
            val session = service.login(request.email, request.password)
                ?: return@post call.respondJson(HttpStatusCode.Unauthorized, errorResponse("Invalid credentials"))
            call.respondJson(body = successResponse("/auth/login", sessionJson(session)))
        }
        post("/refresh") {
            val request = call.receiveOrDefault(RefreshSessionRequest())
            val session = service.refresh(request)
                ?: return@post call.respondJson(HttpStatusCode.Unauthorized, errorResponse("Invalid refresh token"))
            call.respondJson(body = successResponse("/auth/refresh", sessionJson(session)))
        }
        post("/logout") {
            val request = call.receiveOrDefault(LogoutRequest())
            if (!service.logout(request)) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid refresh token"))
            }
            call.respondJson(HttpStatusCode.OK, successResponse("/auth/logout", messageJson("Logout completed.")))
        }
    }
    route("/client/auth") {
        post("/claim-code") {
            val request = call.receiveOrDefault(ClientAccessCodeClaimRequest())
            val session = service.claimClientAccessCode(request)
                ?: return@post call.respondJson(HttpStatusCode.Unauthorized, errorResponse("Invalid or expired client invitation code"))
            call.respondJson(body = successResponse("/client/auth/claim-code", sessionJson(session)))
        }
    }
}

private suspend fun RoutingCall.receiveLoginRequest(): LoginRequest {
    runCatching { receiveNullable<LoginRequest>() }
        .getOrNull()
        ?.let { return it }
    return LoginRequest()
}
