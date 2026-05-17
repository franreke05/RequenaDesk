package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.TicketCloseAcceptanceRequest
import com.requena.supportdesk.server.domain.model.TicketSatisfactionRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.requireClientIdentity
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.ticketJson
import com.requena.supportdesk.server.utils.ticketsJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Route.ticketRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    adminTicketRoutes(service, tokenService)
    clientTicketRoutes(service, tokenService)

    route("/tickets") {
        get {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val limit = call.request.queryParameters.boundedInt("limit", default = 100, max = 200)
            val offset = call.request.queryParameters.boundedInt("offset", default = 0, max = 10_000)
            call.respondJson(body = successResponse("/tickets", ticketsJson(service.tickets(ownerAdminId = identity.userId, limit = limit, offset = offset))))
        }
        get("/{id}") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val id = call.parameters["id"] ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.ticket(id, ownerAdminId = identity.userId)
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            call.respondJson(body = successResponse("/tickets/$id", ticketJson(ticket)))
        }
    }
}

private fun Route.adminTicketRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin/tickets") {
        get {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val limit = call.request.queryParameters.boundedInt("limit", default = 100, max = 200)
            val offset = call.request.queryParameters.boundedInt("offset", default = 0, max = 10_000)
            call.respondJson(body = successResponse("/admin/tickets", ticketsJson(service.tickets(ownerAdminId = identity.userId, limit = limit, offset = offset))))
        }
        get("/{id}") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val id = call.parameters["id"] ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.ticket(id, ownerAdminId = identity.userId)
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            call.respondJson(body = successResponse("/admin/tickets/$id", ticketJson(ticket)))
        }
        post {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val request = call.receiveOrDefault(CreateTicketRequest())
            if (!request.isValidForAdmin()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket payload"))
            }
            call.respondJson(
                HttpStatusCode.Created,
                successResponse("/admin/tickets", ticketJson(service.createdAdminTicket(request, identity.userId))),
            )
        }
        post("/{id}/messages") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val request = call.receiveOrDefault(CreateTicketMessageRequest()).copy(authorId = identity.userId)
            if (request.body.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket message payload"))
            }
            val result = service.createdAdminMessage(id, identity.userId, request)
            call.respondJson(body = ticketMessageCreatedJson("/admin/tickets/$id/messages", result.ticketId, result.messageId))
        }
        patch("/{id}/status") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@patch
            val id = call.parameters["id"] ?: return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.ticket(id, ownerAdminId = identity.userId)
            if (ticket == null) {
                return@patch call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            }
            val request = call.receiveOrDefault(UpdateTicketStatusRequest())
            if (request.status !in allowedStatuses) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket status"))
            }
            val result = service.updatedStatus(ticket.id, request)
            call.respondJson(body = fieldUpdateJson("/admin/tickets/$id/status", "status", result.ticketId, result.value))
        }
        patch("/{id}/priority") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@patch
            val id = call.parameters["id"] ?: return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.ticket(id, ownerAdminId = identity.userId)
            if (ticket == null) {
                return@patch call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            }
            val request = call.receiveOrDefault(UpdateTicketPriorityRequest())
            if (request.priority !in allowedPriorities) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket priority"))
            }
            val result = service.updatedPriority(ticket.id, request)
            call.respondJson(body = fieldUpdateJson("/admin/tickets/$id/priority", "priority", result.ticketId, result.value))
        }
        post("/{id}/accept-close") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val request = call.receiveOrDefault(TicketCloseAcceptanceRequest())
            val ticket = service.acceptedAdminClose(id, identity.userId, identity.userId, request.resolutionSummary)
            call.respondJson(body = successResponse("/admin/tickets/$id/accept-close", ticketJson(ticket)))
        }
        post("/{id}/attachments") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.ticket(id, ownerAdminId = identity.userId)
            if (ticket == null) {
                return@post call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            }
            val request = call.receiveOrDefault(UploadAttachmentRequest()).copy(uploadedBy = identity.userId)
            if (request.fileName.isBlank() || request.storageKey.isBlank() || request.sizeBytes < 0) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid attachment payload"))
            }
            val result = service.uploadedAttachment(ticket.id, request)
            call.respondJson(
                body = successResponse(
                    "/admin/tickets/$id/attachments",
                    buildJsonObject {
                        put("ticketId", result.ticketId)
                        put("attachmentId", result.attachmentId)
                        put("message", "Attachment metadata stored")
                    },
                ),
            )
        }
    }
}

private fun Route.clientTicketRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/client/tickets") {
        get {
            val identity = call.requireClientIdentity(tokenService) ?: return@get
            val clientId = identity.clientId ?: return@get call.respondJson(HttpStatusCode.Forbidden, errorResponse("Client identity is required"))
            val limit = call.request.queryParameters.boundedInt("limit", default = 100, max = 100)
            val offset = call.request.queryParameters.boundedInt("offset", default = 0, max = 10_000)
            call.respondJson(body = successResponse("/client/tickets", ticketsJson(service.clientTickets(clientId, limit = limit, offset = offset))))
        }
        get("/{id}") {
            val identity = call.requireClientIdentity(tokenService) ?: return@get
            val id = call.parameters["id"] ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.ticket(id, clientId = identity.clientId)
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            call.respondJson(body = successResponse("/client/tickets/$id", ticketJson(ticket)))
        }
        post {
            val identity = call.requireClientIdentity(tokenService) ?: return@post
            val request = call.receiveOrDefault(CreateTicketRequest())
            if (!request.isValidForClient()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket payload"))
            }
            val ticket = service.createdClientTicket(request, identity)
            call.respondJson(HttpStatusCode.Created, successResponse("/client/tickets", ticketJson(ticket)))
        }
        post("/{id}/messages") {
            val identity = call.requireClientIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val request = call.receiveOrDefault(CreateTicketMessageRequest()).copy(authorId = identity.userId)
            if (request.body.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket message payload"))
            }
            val result = service.createdClientMessage(id, identity, request)
            call.respondJson(body = ticketMessageCreatedJson("/client/tickets/$id/messages", result.ticketId, result.messageId))
        }
        post("/{id}/accept-close") {
            val identity = call.requireClientIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.acceptedClientClose(id, identity)
            call.respondJson(body = successResponse("/client/tickets/$id/accept-close", ticketJson(ticket)))
        }
        post("/{id}/satisfaction") {
            val identity = call.requireClientIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val request = call.receiveOrDefault(TicketSatisfactionRequest())
            if (request.rating !in 1..5) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Rating must be between 1 and 5"))
            }
            val ticket = service.ratedClientTicket(id, identity, request.rating)
            call.respondJson(body = successResponse("/client/tickets/$id/satisfaction", ticketJson(ticket)))
        }
    }
}

private fun CreateTicketRequest.isValidForAdmin(): Boolean =
    clientId.isNotBlank() &&
        subject.isNotBlank() &&
        description.isNotBlank() &&
        category in allowedTicketCategories &&
        platform in allowedPlatforms &&
        priority in allowedPriorities

private fun CreateTicketRequest.isValidForClient(): Boolean =
    subject.isNotBlank() &&
        description.isNotBlank() &&
        category in allowedTicketCategories &&
        platform in allowedPlatforms

private fun ticketMessageCreatedJson(path: String, ticketId: String, messageId: String) =
    successResponse(
        path,
        buildJsonObject {
            put("ticketId", ticketId)
            put("messageId", messageId)
            put("message", "Reply stored")
        },
    )

private fun fieldUpdateJson(path: String, field: String, ticketId: String, value: String) =
    successResponse(
        path,
        buildJsonObject {
            put("ticketId", ticketId)
            put(field, value)
        },
    )

private val allowedTicketCategories = setOf("BUG", "ACCESS", "BILLING", "CHANGE_REQUEST", "QUESTION", "OTHER")
private val allowedPlatforms = setOf("ANDROID", "IOS", "DESKTOP", "WEB", "BACKEND", "OTHER")
private val allowedPriorities = setOf("LOW", "MEDIUM", "HIGH", "URGENT")
private val allowedStatuses = setOf("OPEN", "IN_PROGRESS", "PENDING_CLIENT", "RESOLVED", "CLOSED")

private fun io.ktor.http.Parameters.boundedInt(name: String, default: Int, max: Int): Int =
    this[name]?.toIntOrNull()?.coerceIn(0, max) ?: default
