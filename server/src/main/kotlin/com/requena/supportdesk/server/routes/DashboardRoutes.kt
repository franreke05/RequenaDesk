package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.dashboardJson
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.dashboardRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin") {
        get("/dashboard") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@get
            val clientId = call.request.queryParameters["clientId"]
            val labelId = call.request.queryParameters["labelId"]
            call.respondJson(
                body = successResponse("/admin/dashboard", dashboardJson(service.dashboard(clientId, labelId, ownerAdminId))),
            )
        }
    }
}
