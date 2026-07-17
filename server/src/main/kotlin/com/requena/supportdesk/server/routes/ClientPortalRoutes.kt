package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.clientJson
import com.requena.supportdesk.server.utils.clientPortalOverviewJson
import com.requena.supportdesk.server.utils.clientTicketsJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.ownerAdminIdFor
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.tasksJson
import com.requena.supportdesk.server.utils.timeLogsJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.clientPortalRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/client") {
        get("/profile") {
            val identity = call.requireClientPortalIdentity(tokenService) ?: return@get
            val client = service.portalClient(identity) ?: return@get call.respondClientPortalNotFound()
            call.respondJson(body = successResponse("/client/profile", clientJson(client)))
        }
        get("/tickets") {
            val identity = call.requireClientPortalIdentity(tokenService) ?: return@get
            val client = service.portalClient(identity) ?: return@get call.respondClientPortalNotFound()
            val tickets = service.tickets().filter { it.clientId == client.id }
            call.respondJson(body = successResponse("/client/tickets", clientTicketsJson(tickets)))
        }
        get("/tasks") {
            val identity = call.requireClientPortalIdentity(tokenService) ?: return@get
            val client = service.portalClient(identity) ?: return@get call.respondClientPortalNotFound()
            val ownerAdminId = service.ownerAdminIdFor(identity) ?: return@get call.respondClientPortalNotFound()
            call.respondJson(
                body = successResponse("/client/tasks", tasksJson(service.tasks(clientId = client.id, ownerAdminId = ownerAdminId))),
            )
        }
        get("/time-logs") {
            val identity = call.requireClientPortalIdentity(tokenService) ?: return@get
            val client = service.portalClient(identity) ?: return@get call.respondClientPortalNotFound()
            val ownerAdminId = service.ownerAdminIdFor(identity) ?: return@get call.respondClientPortalNotFound()
            call.respondJson(
                body = successResponse("/client/time-logs", timeLogsJson(service.timeLogs(clientId = client.id, ownerAdminId = ownerAdminId))),
            )
        }
        get("/overview") {
            val identity = call.requireClientPortalIdentity(tokenService) ?: return@get
            val client = service.portalClient(identity) ?: return@get call.respondClientPortalNotFound()
            val ownerAdminId = service.ownerAdminIdFor(identity) ?: return@get call.respondClientPortalNotFound()
            call.respondJson(
                body = successResponse(
                    "/client/overview",
                    clientPortalOverviewJson(
                        client = client,
                        tickets = service.tickets().filter { it.clientId == client.id },
                        tasks = service.tasks(clientId = client.id, ownerAdminId = ownerAdminId),
                        timeLogs = service.timeLogs(clientId = client.id, ownerAdminId = ownerAdminId),
                    ),
                ),
            )
        }
    }
}

private suspend fun ApplicationCall.requireClientPortalIdentity(
    tokenService: SupportDeskTokenService,
): ServerAuthIdentity? {
    val identity = requireAuthenticatedIdentity(tokenService) ?: return null
    if (identity.role == "CLIENT" && !identity.clientId.isNullOrBlank()) return identity
    respondJson(HttpStatusCode.Forbidden, errorResponse("Client portal access is required"))
    return null
}

private fun SupportDeskService.portalClient(identity: ServerAuthIdentity): ServerClientSnapshot? =
    identity.clientId?.let { clientId -> clients().firstOrNull { it.id == clientId } }

private suspend fun ApplicationCall.respondClientPortalNotFound() {
    respondJson(HttpStatusCode.NotFound, errorResponse("Client portal is not linked to an active client"))
}
