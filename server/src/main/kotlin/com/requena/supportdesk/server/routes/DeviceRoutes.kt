package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.service.SupportDeskPlaceholderService
import com.requena.supportdesk.server.utils.deviceJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.deviceRoutes(service: SupportDeskPlaceholderService) {
    route("/devices") {
        post("/register") {
            val request = runCatching { call.receive<RegisterDeviceRequest>() }.getOrDefault(RegisterDeviceRequest())
            if (request.platform !in allowedDevicePlatforms) {
                return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Invalid device platform"))
            }
            call.respond(HttpStatusCode.Created, successResponse("/devices/register", deviceJson(service.registerDevice(request))))
        }
    }
}

private val allowedDevicePlatforms = setOf("ANDROID", "IOS", "DESKTOP", "WEB")
