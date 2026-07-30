package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.StartWorkSessionRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.workSessionJson
import com.requena.supportdesk.server.utils.workSessionsJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.JsonNull

/**
 * The global timer for a tenant (owner_admin_id): start/pause/resume/stop/checkpoint. There can
 * only ever be one non-finished (ACTIVE/PAUSED) session per tenant - enforced here, in the
 * repository, and by a partial unique index in Postgres (see V13 migration).
 */
fun Route.workSessionRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin/work-sessions") {
        get("/active") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val session = service.activeWorkSession(identity.userId)
            call.respondJson(
                body = successResponse(
                    "/admin/work-sessions/active",
                    session?.let { workSessionJson(it) } ?: JsonNull,
                ),
            )
        }
        post {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val request = call.receiveOrDefault(StartWorkSessionRequest())
            if (request.taskId.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task id is required"))
            }
            call.respondJson(
                HttpStatusCode.Created,
                successResponse(
                    "/admin/work-sessions",
                    workSessionJson(service.startedWorkSession(request, identity.userId, identity.userId)),
                ),
            )
        }
        post("/{sessionId}/pause") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val sessionId = call.parameters["sessionId"].orEmpty()
            call.respondJson(
                body = successResponse(
                    "/admin/work-sessions/$sessionId/pause",
                    workSessionJson(service.pausedWorkSession(sessionId, identity.userId)),
                ),
            )
        }
        post("/{sessionId}/resume") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val sessionId = call.parameters["sessionId"].orEmpty()
            call.respondJson(
                body = successResponse(
                    "/admin/work-sessions/$sessionId/resume",
                    workSessionJson(service.resumedWorkSession(sessionId, identity.userId)),
                ),
            )
        }
        post("/{sessionId}/stop") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val sessionId = call.parameters["sessionId"].orEmpty()
            call.respondJson(
                body = successResponse(
                    "/admin/work-sessions/$sessionId/stop",
                    workSessionJson(service.stoppedWorkSession(sessionId, identity.userId)),
                ),
            )
        }
        post("/{sessionId}/checkpoint") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val sessionId = call.parameters["sessionId"].orEmpty()
            call.respondJson(
                body = successResponse(
                    "/admin/work-sessions/$sessionId/checkpoint",
                    workSessionJson(service.checkpointedWorkSession(sessionId, identity.userId)),
                ),
            )
        }
    }
    route("/admin/tasks/{taskId}/work-sessions") {
        get {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val taskId = call.parameters["taskId"].orEmpty()
            call.respondJson(
                body = successResponse(
                    "/admin/tasks/$taskId/work-sessions",
                    workSessionsJson(service.workSessionsForTask(taskId, identity.userId)),
                ),
            )
        }
    }
}
