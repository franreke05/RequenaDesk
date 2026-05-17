package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAdminIdentity
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
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Route.timeLogRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin") {
        get("/time-logs") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@get
            val clientId = call.request.queryParameters["clientId"]
            val taskId = call.request.queryParameters["taskId"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 100
            val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceIn(0, 10_000) ?: 0
            call.respondJson(
                body = successResponse("/admin/time-logs", timeLogsJson(service.timeLogs(clientId, taskId, ownerAdminId, limit, offset))),
            )
        }
        get("/time-logs/summary") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@get
            val clientId = call.request.queryParameters["clientId"]
            val from = call.request.queryParameters["from"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val to = call.request.queryParameters["to"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val groupBy = call.request.queryParameters["groupBy"]?.lowercase() ?: "month"
            if (groupBy !in setOf("day", "week", "month")) {
                return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid groupBy"))
            }
            val logs = service.timeLogs(clientId = clientId, ownerAdminId = ownerAdminId, limit = 1_000)
                .filter { log ->
                    val date = runCatching { LocalDate.parse(log.workDate) }.getOrNull()
                    date != null &&
                        (from == null || !date.isBefore(from)) &&
                        (to == null || !date.isAfter(to))
                }
            val groups = logs.groupBy { log -> log.workDate.summaryKey(groupBy) }
            call.respondJson(
                body = successResponse(
                    "/admin/time-logs/summary",
                    buildJsonArray {
                        groups.toSortedMap().forEach { (period, periodLogs) ->
                            add(
                                buildJsonObject {
                                    put("period", period)
                                    put("minutes", periodLogs.sumOf { it.minutes })
                                    put("seconds", periodLogs.sumOf { it.seconds })
                                    put("billableMinutes", periodLogs.filter { it.billable }.sumOf { it.minutes })
                                    put("entries", periodLogs.size)
                                },
                            )
                        }
                    },
                ),
            )
        }
        post("/time-logs") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val ownerAdminId = identity.userId
            val request = call.receiveOrDefault(CreateTimeLogRequest()).copy(authorId = identity.userId)
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

private fun String.summaryKey(groupBy: String): String {
    val date = LocalDate.parse(this)
    return when (groupBy) {
        "day" -> date.toString()
        "week" -> {
            val fields = WeekFields.of(Locale.ROOT)
            "${date.year}-W${date.get(fields.weekOfWeekBasedYear()).toString().padStart(2, '0')}"
        }
        else -> "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
    }
}
