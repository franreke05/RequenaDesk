package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.utils.orgId

import com.requena.supportdesk.server.domain.model.UpdateTaskBoardPlacementRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.taskBoardJson
import com.requena.supportdesk.server.utils.taskBoardPlacementJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.taskBoardRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin/task-board") {
        get {
            val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@get
            call.respondJson(
                body = successResponse("/admin/task-board", taskBoardJson(service.taskBoard(organizationId))),
            )
        }

        route("/placements/{taskId}") {
            put {
                val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@put
                val taskId = call.parameters["taskId"].orEmpty()
                if (taskId.isBlank()) {
                    return@put call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task id is required"))
                }
                val request = call.receiveOrDefault(UpdateTaskBoardPlacementRequest())
                val placement = service.placedTaskOnBoard(
                    taskId = taskId,
                    organizationId = organizationId,
                    boardDate = request.boardDate,
                    positionX = request.positionX,
                    positionY = request.positionY,
                )
                call.respondJson(
                    body = successResponse(
                        "/admin/task-board/placements/$taskId",
                        taskBoardPlacementJson(placement),
                    ),
                )
            }

            delete {
                val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@delete
                val taskId = call.parameters["taskId"].orEmpty()
                if (taskId.isBlank()) {
                    return@delete call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task id is required"))
                }
                service.removedTaskFromBoard(taskId, organizationId)
                call.respondJson(
                    body = successResponse(
                        "/admin/task-board/placements/$taskId",
                        messageJson("Task returned to the planning inbox"),
                    ),
                )
            }
        }
    }
}
