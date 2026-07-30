package com.requena.supportdesk.server.business.projects

import com.requena.supportdesk.features.business.projects.BusinessMutationResult
import com.requena.supportdesk.features.business.projects.BusinessProjectStatus
import com.requena.supportdesk.features.business.projects.BusinessProjectsValidationException
import com.requena.supportdesk.features.business.projects.BusinessTaskBoardPlacementInput
import com.requena.supportdesk.features.business.projects.CreateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.CreateBusinessTaskLabelInput
import com.requena.supportdesk.features.business.projects.ProjectTransitionInput
import com.requena.supportdesk.features.business.projects.StartBusinessWorkSessionInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessProjectInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskInput
import com.requena.supportdesk.features.business.projects.UpdateBusinessTaskLabelInput
import com.requena.supportdesk.features.business.projects.BusinessWorkSession
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.util.UUID

fun Route.businessProjectsRoutes(
    service: BusinessProjectsService,
    tokenService: SupportDeskTokenService,
    supportDeskService: SupportDeskService,
) {
    route("/client/apps") {
        route("/projects") {
            get {
                val identity = call.projectsIdentity(tokenService) ?: return@get
                val query = call.projectsQuery() ?: return@get
                call.respondProjects("/client/apps/projects") { service.projects(identity, query) }
            }
            get("/workspace") {
                val identity = call.projectsIdentity(tokenService) ?: return@get
                val query = call.projectsQuery() ?: return@get
                call.respondProjects("/client/apps/projects/workspace") { service.workspace(identity, query) }
            }
            post {
                val identity = call.projectsIdentity(tokenService) ?: return@post
                val input = call.receiveProjects<CreateBusinessProjectInput>() ?: return@post
                call.respondProjects("/client/apps/projects", HttpStatusCode.Created) { service.createProject(identity, input) }
            }
            get("/{projectId}") {
                val identity = call.projectsIdentity(tokenService) ?: return@get
                val id = call.uuidParameter("projectId") ?: return@get
                call.respondProjects("/client/apps/projects/$id") { service.project(identity, id) }
            }
            patch("/{projectId}") {
                val identity = call.projectsIdentity(tokenService) ?: return@patch
                val id = call.uuidParameter("projectId") ?: return@patch
                val input = call.receiveProjects<UpdateBusinessProjectInput>() ?: return@patch
                call.respondProjects("/client/apps/projects/$id") { service.updateProject(identity, id, input) }
            }
            projectTransition("complete", BusinessProjectStatus.COMPLETED, service, tokenService)
            projectTransition("reopen", BusinessProjectStatus.ACTIVE, service, tokenService)
            projectTransition("archive", BusinessProjectStatus.ARCHIVED, service, tokenService)
        }

        route("/tasks") {
            get {
                val identity = call.projectsIdentity(tokenService) ?: return@get
                val query = call.projectsQuery() ?: return@get
                call.respondProjects("/client/apps/tasks") { service.tasks(identity, query) }
            }
            post {
                val identity = call.projectsIdentity(tokenService) ?: return@post
                val input = call.receiveProjects<CreateBusinessTaskInput>() ?: return@post
                call.respondProjects("/client/apps/tasks", HttpStatusCode.Created) { service.createTask(identity, input) }
            }
            route("/labels") {
                post {
                    val identity = call.projectsIdentity(tokenService) ?: return@post
                    val input = call.receiveProjects<CreateBusinessTaskLabelInput>() ?: return@post
                    call.respondProjects("/client/apps/tasks/labels", HttpStatusCode.Created) { service.createLabel(identity, input) }
                }
                patch("/{labelId}") {
                    val identity = call.projectsIdentity(tokenService) ?: return@patch
                    val id = call.uuidParameter("labelId") ?: return@patch
                    val input = call.receiveProjects<UpdateBusinessTaskLabelInput>() ?: return@patch
                    call.respondProjects("/client/apps/tasks/labels/$id") { service.updateLabel(identity, id, input) }
                }
                post("/{labelId}/archive") {
                    val identity = call.projectsIdentity(tokenService) ?: return@post
                    val id = call.uuidParameter("labelId") ?: return@post
                    val input = call.receiveProjects<ProjectTransitionInput>() ?: return@post
                    call.respondProjects("/client/apps/tasks/labels/$id/archive") { service.archiveLabel(identity, id, input.expectedVersion) }
                }
            }
            get("/{taskId}") {
                val identity = call.projectsIdentity(tokenService) ?: return@get
                val id = call.uuidParameter("taskId") ?: return@get
                call.respondProjects("/client/apps/tasks/$id") { service.task(identity, id) }
            }
            patch("/{taskId}") {
                val identity = call.projectsIdentity(tokenService) ?: return@patch
                val id = call.uuidParameter("taskId") ?: return@patch
                val input = call.receiveProjects<UpdateBusinessTaskInput>() ?: return@patch
                call.respondProjects("/client/apps/tasks/$id") { service.updateTask(identity, id, input) }
            }
            get("/{taskId}/work-sessions") {
                val identity = call.projectsIdentity(tokenService) ?: return@get
                val id = call.uuidParameter("taskId") ?: return@get
                call.respondProjects("/client/apps/tasks/$id/work-sessions") { service.taskWorkSessions(identity, id) }
            }
        }

        route("/task-board") {
            get {
                val identity = call.projectsIdentity(tokenService) ?: return@get
                call.respondProjects("/client/apps/task-board") { service.board(identity) }
            }
            route("/placements/{taskId}") {
                put {
                    val identity = call.projectsIdentity(tokenService) ?: return@put
                    val id = call.uuidParameter("taskId") ?: return@put
                    val input = call.receiveProjects<BusinessTaskBoardPlacementInput>() ?: return@put
                    call.respondProjects("/client/apps/task-board/placements/$id") { service.placeTask(identity, id, input) }
                }
                delete {
                    val identity = call.projectsIdentity(tokenService) ?: return@delete
                    val id = call.uuidParameter("taskId") ?: return@delete
                    call.respondProjects("/client/apps/task-board/placements/$id") {
                        service.removeTaskPlacement(identity, id)
                        BusinessMutationResult()
                    }
                }
            }
        }

        route("/work-sessions") {
            get("/active") {
                val identity = call.projectsIdentity(tokenService) ?: return@get
                call.respondProjects("/client/apps/work-sessions/active") { service.activeWorkSession(identity) }
            }
            post("/start") {
                val identity = call.projectsIdentity(tokenService) ?: return@post
                val input = call.receiveProjects<StartBusinessWorkSessionInput>() ?: return@post
                if (!input.taskId.isUuid()) {
                    call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid task identifier"))
                    return@post
                }
                call.respondProjects("/client/apps/work-sessions/start", HttpStatusCode.Created) { service.startWorkSession(identity, input.taskId) }
            }
            workSessionAction("pause", tokenService, service::pauseWorkSession)
            workSessionAction("resume", tokenService, service::resumeWorkSession)
            workSessionAction("stop", tokenService, service::stopWorkSession)
            workSessionAction("checkpoint", tokenService, service::checkpointWorkSession)
        }
    }

    get("/admin/clients/{clientId}/business-program-metrics") {
        val admin = call.requireAdminIdentity(tokenService) ?: return@get
        val clientId = call.uuidParameter("clientId") ?: return@get
        if (supportDeskService.clients(admin.userId).none { it.id == clientId }) {
            call.respondJson(HttpStatusCode.NotFound, errorResponse("Client was not found"))
            return@get
        }
        call.respondProjects("/admin/clients/$clientId/business-program-metrics") { service.metrics(clientId) }
    }
}

private fun Route.projectTransition(
    segment: String,
    target: BusinessProjectStatus,
    service: BusinessProjectsService,
    tokenService: SupportDeskTokenService,
) {
    post("/{projectId}/$segment") {
        val identity = call.projectsIdentity(tokenService) ?: return@post
        val id = call.uuidParameter("projectId") ?: return@post
        val input = call.receiveProjects<ProjectTransitionInput>() ?: return@post
        call.respondProjects("/client/apps/projects/$id/$segment") {
            service.transitionProject(identity, id, target, input.expectedVersion)
        }
    }
}

private fun Route.workSessionAction(
    segment: String,
    tokenService: SupportDeskTokenService,
    operation: (BusinessProjectsIdentity, String) -> BusinessWorkSession,
) {
    post("/{sessionId}/$segment") {
        val identity = call.projectsIdentity(tokenService) ?: return@post
        val id = call.uuidParameter("sessionId") ?: return@post
        call.respondProjects("/client/apps/work-sessions/$id/$segment") { operation(identity, id) }
    }
}

private suspend fun ApplicationCall.projectsIdentity(tokenService: SupportDeskTokenService): BusinessProjectsIdentity? {
    val identity = requireAuthenticatedIdentity(tokenService) ?: return null
    if (identity.role == "CLIENT" && !identity.clientId.isNullOrBlank()) {
        return BusinessProjectsIdentity(identity.userId, requireNotNull(identity.clientId))
    }
    respondJson(HttpStatusCode.Forbidden, errorResponse("Client portal access is required"))
    return null
}

private suspend fun ApplicationCall.projectsQuery(): BusinessProjectsQuery? {
    val query = BusinessProjectsQuery(
        query = request.queryParameters["query"]?.trim()?.takeIf(String::isNotBlank),
        projectStatus = request.queryParameters["projectStatus"]?.uppercase()?.takeIf(String::isNotBlank),
        taskStatus = request.queryParameters["taskStatus"]?.uppercase()?.takeIf(String::isNotBlank),
        projectId = request.queryParameters["projectId"]?.takeIf(String::isNotBlank),
        customerId = request.queryParameters["customerId"]?.takeIf(String::isNotBlank),
        labelId = request.queryParameters["labelId"]?.takeIf(String::isNotBlank),
    )
    if (
        query.query?.length?.let { it > 160 } == true ||
        listOf(query.projectId, query.customerId, query.labelId).filterNotNull().any { !it.isUuid() }
    ) {
        respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid list filters"))
        return null
    }
    return query
}

private suspend fun ApplicationCall.uuidParameter(name: String): String? {
    val value = parameters[name].orEmpty()
    if (value.isUuid()) return value
    respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid identifier"))
    return null
}

private suspend inline fun <reified T : Any> ApplicationCall.receiveProjects(): T? = try {
    receive<T>()
} catch (_: Throwable) {
    respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid request payload"))
    null
}

private suspend inline fun <reified T> ApplicationCall.respondProjects(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    block: () -> T,
) {
    try {
        respondJson(status, successResponse(path, Json.encodeToJsonElement(block())))
    } catch (_: BusinessProjectsForbiddenException) {
        respondJson(HttpStatusCode.Forbidden, errorResponse("Projects program access is required"))
    } catch (_: BusinessProjectsNotFoundException) {
        respondJson(HttpStatusCode.NotFound, errorResponse("Business project record was not found"))
    } catch (error: BusinessProjectsConflictException) {
        respondJson(HttpStatusCode.Conflict, errorResponse(error.message ?: "Business project conflict"))
    } catch (error: BusinessProjectsValidationException) {
        respondJson(HttpStatusCode.UnprocessableEntity, errorResponse(error.message ?: "Invalid business project data"))
    } catch (error: IllegalArgumentException) {
        respondJson(HttpStatusCode.UnprocessableEntity, errorResponse(error.message ?: "Invalid business project data"))
    }
}

private fun String.isUuid(): Boolean = runCatching { UUID.fromString(this) }.isSuccess
