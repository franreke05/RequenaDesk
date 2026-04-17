package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.adminOwnerId
import com.requena.supportdesk.server.utils.dashboardJson
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.dashboardRoutes(service: SupportDeskService) {
    route("/admin") {
        get("/dashboard") {
            val clientId = call.request.queryParameters["clientId"]
            val labelId = call.request.queryParameters["labelId"]
            call.respondJson(
                body = successResponse("/admin/dashboard", dashboardJson(service.dashboard(clientId, labelId, call.adminOwnerId()))),
            )
        }
    }
}
