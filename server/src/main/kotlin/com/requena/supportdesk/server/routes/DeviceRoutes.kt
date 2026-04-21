package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.deviceJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.deviceRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/devices") {
        post("/register") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
            val request = call.receiveOrDefault(RegisterDeviceRequest()).copy(userId = identity.userId)
            if (request.userId.isBlank() || request.token.isBlank() || request.platform !in allowedDevicePlatforms) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid device platform"))
            }
            call.respondJson(HttpStatusCode.Created, successResponse("/devices/register", deviceJson(service.registerDevice(request))))
        }
    }
}

private val allowedDevicePlatforms = setOf("ANDROID", "IOS")
