package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.deviceJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.deviceRoutes(service: SupportDeskService) {
    route("/devices") {
        post("/register") {
            val request = call.receiveOrDefault(RegisterDeviceRequest())
            if (request.userId.isBlank() || request.token.isBlank() || request.platform !in allowedDevicePlatforms) {
                return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Invalid device platform"))
            }
            call.respond(HttpStatusCode.Created, successResponse("/devices/register", deviceJson(service.registerDevice(request))))
        }
    }
}

private val allowedDevicePlatforms = setOf("ANDROID", "IOS")
