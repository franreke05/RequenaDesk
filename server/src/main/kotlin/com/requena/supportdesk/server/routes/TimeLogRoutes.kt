package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAdminOwnerId
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.timeLogJson
import com.requena.supportdesk.server.utils.timeLogsJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.timeLogRoutes(service: SupportDeskService) {
    route("/admin") {
        get("/time-logs") {
            val ownerAdminId = call.requireAdminOwnerId() ?: return@get
            val clientId = call.request.queryParameters["clientId"]
            val taskId = call.request.queryParameters["taskId"]
            call.respondJson(
                body = successResponse("/admin/time-logs", timeLogsJson(service.timeLogs(clientId, taskId, ownerAdminId))),
            )
        }
        post("/time-logs") {
            val ownerAdminId = call.requireAdminOwnerId() ?: return@post
            val request = call.receiveOrDefault(CreateTimeLogRequest())
            val resolvedSeconds = request.seconds.takeIf { it > 0 } ?: (request.minutes * 60)
            if (request.taskId.isBlank() || request.authorId.isBlank() || request.workDate.isBlank() || resolvedSeconds <= 0) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task, author, date and time are required"))
            }
            call.respondJson(
                HttpStatusCode.Created,
                successResponse("/admin/time-logs", timeLogJson(service.createdTimeLog(request, ownerAdminId))),
            )
        }
    }
}
