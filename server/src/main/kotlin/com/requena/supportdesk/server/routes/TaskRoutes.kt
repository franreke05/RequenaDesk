package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.utils.orgId

import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.isAdmin
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
import java.time.LocalDate

private const val clientDailyTaskLimit = 5

fun Route.taskRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin") {
        get("/tasks") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            val organizationId = identity.orgId
                ?: return@get call.respondJson(HttpStatusCode.Forbidden, errorResponse("No client account is linked to this user"))
            val clientId = if (identity.isAdmin) call.request.queryParameters["clientId"] else identity.clientId
            val labelId = call.request.queryParameters["labelId"]
            call.respondJson(
                body = successResponse("/admin/tasks", tasksJson(service.tasks(clientId, labelId, organizationId))),
            )
        }
        post("/tasks") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
            val organizationId = identity.orgId
                ?: return@post call.respondJson(HttpStatusCode.Forbidden, errorResponse("No client account is linked to this user"))
            val request = call.receiveOrDefault(CreateTaskRequest()).let { received ->
                if (identity.isAdmin) received else received.copy(clientId = identity.clientId)
            }
            if (request.title.isBlank() || request.labelId.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task title and label are required"))
            }
            if (!identity.isAdmin) {
                val today = LocalDate.now().toString()
                val tasksCreatedToday = service.tasks(clientId = identity.clientId, organizationId = organizationId)
                    .count { it.createdAt.take(10) == today }
                if (tasksCreatedToday >= clientDailyTaskLimit) {
                    return@post call.respondJson(
                        HttpStatusCode.Conflict,
                        errorResponse("Daily task limit reached"),
                    )
                }
            }
            call.respondJson(
                HttpStatusCode.Created,
                successResponse(
                    "/admin/tasks",
                    taskJson(service.createdTask(request, identity.userId, organizationId)),
                ),
            )
        }
        patch("/tasks/{taskId}") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@patch
            val organizationId = identity.orgId
                ?: return@patch call.respondJson(HttpStatusCode.Forbidden, errorResponse("No client account is linked to this user"))
            val taskId = call.parameters["taskId"].orEmpty()
            if (taskId.isBlank()) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task id is required"))
            }
            val currentTask = service.tasks(
                clientId = identity.clientId.takeUnless { identity.isAdmin },
                organizationId = organizationId,
            ).firstOrNull { it.id == taskId }
                ?: return@patch call.respondJson(HttpStatusCode.NotFound, errorResponse("Task not found"))
            val received = call.receiveOrDefault(UpdateTaskRequest())
            val request = if (identity.isAdmin) {
                received
            } else {
                UpdateTaskRequest(completed = received.completed ?: currentTask.completed)
            }
            call.respondJson(
                body = successResponse("/admin/tasks/$taskId", taskJson(service.updatedTask(taskId, request, organizationId))),
            )
        }
        delete("/tasks/{taskId}") {
            val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@delete
            val taskId = call.parameters["taskId"].orEmpty()
            if (taskId.isBlank()) {
                return@delete call.respondJson(HttpStatusCode.BadRequest, errorResponse("Task id is required"))
            }
            service.deletedTask(taskId, organizationId)
            call.respondJson(body = successResponse("/admin/tasks/$taskId", messageJson("Task deleted")))
        }
    }
}
