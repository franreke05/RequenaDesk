package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.adminOwnerId
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.taskJson
import com.requena.supportdesk.server.utils.tasksJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.taskRoutes(service: SupportDeskService) {
    route("/admin") {
        get("/tasks") {
            val clientId = call.request.queryParameters["clientId"]
            val labelId = call.request.queryParameters["labelId"]
            call.respondJson(
                body = successResponse("/admin/tasks", tasksJson(service.tasks(clientId, labelId, call.adminOwnerId()))),
            )
        }
        post("/tasks") {
            val ownerAdminId = call.adminOwnerId()
            val request = call.receiveOrDefault(CreateTaskRequest())
            if (request.title.isBlank() || request.labelId.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task title and label are required"))
            }
            call.respondJson(
                HttpStatusCode.Created,
                successResponse("/admin/tasks", taskJson(service.createdTask(request, ownerAdminId))),
            )
        }
        patch("/tasks/{taskId}") {
            val ownerAdminId = call.adminOwnerId()
            val taskId = call.parameters["taskId"].orEmpty()
            if (taskId.isBlank()) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task id is required"))
            }
            val request = call.receiveOrDefault(UpdateTaskRequest())
            call.respondJson(
                body = successResponse("/admin/tasks/$taskId", taskJson(service.updatedTask(taskId, request, ownerAdminId))),
            )
        }
        delete("/tasks/{taskId}") {
            val ownerAdminId = call.adminOwnerId()
            val taskId = call.parameters["taskId"].orEmpty()
            if (taskId.isBlank()) {
                return@delete call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task id is required"))
            }
            service.deletedTask(taskId, ownerAdminId)
            call.respondJson(body = successResponse("/admin/tasks/$taskId", messageJson("Task deleted")))
        }
    }
}
