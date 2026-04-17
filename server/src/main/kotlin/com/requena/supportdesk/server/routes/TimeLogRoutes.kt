package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.timeLogJson
import com.requena.supportdesk.server.utils.timeLogsJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.timeLogRoutes(service: SupportDeskService) {
    route("/admin") {
        get("/time-logs") {
            val clientId = call.request.queryParameters["clientId"]
            val taskId = call.request.queryParameters["taskId"]
            call.respond(successResponse("/admin/time-logs", timeLogsJson(service.timeLogs(clientId, taskId))))
        }
        post("/time-logs") {
            val request = call.receiveOrDefault(CreateTimeLogRequest())
            if (request.taskId.isBlank() || request.authorId.isBlank() || request.workDate.isBlank() || request.minutes <= 0) {
                return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Task, author, date and minutes are required"))
            }
            call.respond(HttpStatusCode.Created, successResponse("/admin/time-logs", timeLogJson(service.createdTimeLog(request))))
        }
    }
}
