package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.isAdmin
import com.requena.supportdesk.server.utils.ownerAdminIdFor
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.timeLogsJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

// time_logs rows are created exclusively by the work-session engine (see WorkSessionRoutes -
// stoppedWorkSession) from real segment timestamps. There is intentionally no POST here: manual
// time-log creation would let someone log arbitrary, unverified minutes outside of any session.
fun Route.timeLogRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin") {
        get("/time-logs") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            val ownerAdminId = service.ownerAdminIdFor(identity)
                ?: return@get call.respondJson(HttpStatusCode.Forbidden, errorResponse("No client account is linked to this user"))
            val clientId = if (identity.isAdmin) call.request.queryParameters["clientId"] else identity.clientId
            val taskId = call.request.queryParameters["taskId"]
            call.respondJson(
                body = successResponse("/admin/time-logs", timeLogsJson(service.timeLogs(clientId, taskId, ownerAdminId))),
            )
        }
    }
}
