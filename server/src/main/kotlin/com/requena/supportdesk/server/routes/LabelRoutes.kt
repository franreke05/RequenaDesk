package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.labelJson
import com.requena.supportdesk.server.utils.labelsJson
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.labelRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin") {
        get("/labels") {
            call.requireAdminIdentity(tokenService) ?: return@get
            call.respondJson(body = successResponse("/admin/labels", labelsJson(service.taskLabels())))
        }
        post("/labels") {
            call.requireAdminIdentity(tokenService) ?: return@post
            val request = call.receiveOrDefault(CreateTaskLabelRequest()).copy(ownerAdminId = "")
            if (request.name.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Label name is required"))
            }
            call.respondJson(
                HttpStatusCode.Created,
                successResponse("/admin/labels", labelJson(service.createdTaskLabel(request))),
            )
        }
        patch("/labels/{labelId}") {
            call.requireAdminIdentity(tokenService) ?: return@patch
            val labelId = call.parameters["labelId"].orEmpty()
            if (labelId.isBlank()) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Label id is required"))
            }
            val request = call.receiveOrDefault(UpdateTaskLabelRequest())
            call.respondJson(
                body = successResponse(
                    "/admin/labels/$labelId",
                    labelJson(service.updatedTaskLabel(labelId, request)),
                ),
            )
        }
        delete("/labels/{labelId}") {
            call.requireAdminIdentity(tokenService) ?: return@delete
            val labelId = call.parameters["labelId"].orEmpty()
            if (labelId.isBlank()) {
                return@delete call.respondJson(HttpStatusCode.BadRequest, errorResponse("Label id is required"))
            }
            service.deletedTaskLabel(labelId)
            call.respondJson(body = successResponse("/admin/labels/$labelId", messageJson("Label deleted")))
        }
    }
}
