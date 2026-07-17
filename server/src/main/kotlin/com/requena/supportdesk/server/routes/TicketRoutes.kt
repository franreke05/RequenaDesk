package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.isAdmin
import com.requena.supportdesk.server.utils.visibleClientIdsFor
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.ticketJson
import com.requena.supportdesk.server.utils.ticketsJson
import com.requena.supportdesk.server.utils.clientTicketJson
import com.requena.supportdesk.server.utils.clientTicketsJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

private const val clientDailyUrgentTicketLimit = 3

fun Route.ticketRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/tickets") {
        get {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            val visibleClientIds = service.visibleClientIdsFor(identity)
            val tickets = service.tickets().filter { it.clientId in visibleClientIds }
            call.respondJson(
                body = successResponse(
                    "/tickets",
                    if (identity.isAdmin) ticketsJson(tickets) else clientTicketsJson(tickets),
                ),
            )
        }
        get("/{id}") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            val id = call.parameters["id"] ?: return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.ticket(id)
                ?.takeIf { it.clientId in service.visibleClientIdsFor(identity) }
                ?: return@get call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            call.respondJson(
                body = successResponse(
                    "/tickets/$id",
                    if (identity.isAdmin) ticketJson(ticket) else clientTicketJson(ticket),
                ),
            )
        }
        post {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
            val received = call.receiveOrDefault(CreateTicketRequest())
            val request = if (identity.isAdmin) {
                received
            } else {
                received.copy(
                    clientId = identity.clientId.orEmpty(),
                    requesterId = identity.userId,
                )
            }
            if (
                request.clientId.isBlank() ||
                request.clientId !in service.visibleClientIdsFor(identity) ||
                request.subject.isBlank() ||
                request.description.isBlank() ||
                request.category !in allowedTicketCategories ||
                request.platform !in allowedPlatforms ||
                request.priority !in allowedPriorities
            ) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket payload"))
            }
            if (!identity.isAdmin && request.priority == "URGENT") {
                val today = LocalDate.now().toString()
                val urgentToday = service.tickets().count {
                    it.clientId == identity.clientId &&
                        it.priority == "URGENT" &&
                        it.createdAt.take(10) == today
                }
                if (urgentToday >= clientDailyUrgentTicketLimit) {
                    return@post call.respondJson(
                        HttpStatusCode.Conflict,
                        errorResponse("Daily urgent ticket limit reached"),
                    )
                }
            }
            val createdTicket = service.createdTicket(request)
            call.respondJson(
                HttpStatusCode.Created,
                successResponse(
                    "/tickets",
                    if (identity.isAdmin) ticketJson(createdTicket) else clientTicketJson(createdTicket),
                ),
            )
        }
        post("/{id}/messages") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            service.ticket(id)
                ?.takeIf { it.clientId in service.visibleClientIdsFor(identity) }
                ?: return@post call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            val request = call.receiveOrDefault(CreateTicketMessageRequest()).copy(authorId = identity.userId)
            if (request.authorId.isBlank() || request.body.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket message payload"))
            }
            val result = service.createdMessage(id, request)
            call.respondJson(
                body =
                successResponse(
                    "/tickets/$id/messages",
                    buildJsonObject {
                        put("ticketId", result.ticketId)
                        put("messageId", result.messageId)
                        put("message", "Reply stored")
                    },
                ),
            )
        }
        patch("/{id}/status") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@patch
            val id = call.parameters["id"] ?: return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            service.ticket(id)
                ?.takeIf { it.clientId in service.visibleClientIdsFor(identity) }
                ?: return@patch call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            val request = call.receiveOrDefault(UpdateTicketStatusRequest())
            if (request.status !in allowedStatuses) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket status"))
            }
            val result = service.updatedStatus(id, request)
            call.respondJson(
                body =
                successResponse(
                    "/tickets/$id/status",
                    buildJsonObject {
                        put("ticketId", result.ticketId)
                        put("status", result.value)
                    },
                ),
            )
        }
        patch("/{id}/priority") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@patch
            val id = call.parameters["id"] ?: return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            service.ticket(id)
                ?.takeIf { it.clientId in service.visibleClientIdsFor(identity) }
                ?: return@patch call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            val request = call.receiveOrDefault(UpdateTicketPriorityRequest())
            if (request.priority !in allowedPriorities) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid ticket priority"))
            }
            val result = service.updatedPriority(id, request)
            call.respondJson(
                body =
                successResponse(
                    "/tickets/$id/priority",
                    buildJsonObject {
                        put("ticketId", result.ticketId)
                        put("priority", result.value)
                    },
                ),
            )
        }
        post("/{id}/attachments") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val id = call.parameters["id"] ?: return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            service.ticket(id)
                ?.takeIf { it.clientId in service.visibleClientIdsFor(identity) }
                ?: return@post call.respondJson(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            val request = call.receiveOrDefault(UploadAttachmentRequest()).copy(uploadedBy = identity.userId)
            if (
                request.uploadedBy.isBlank() ||
                request.fileName.isBlank() ||
                request.storageKey.isBlank() ||
                request.sizeBytes < 0
            ) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid attachment payload"))
            }
            val result = service.uploadedAttachment(id, request)
            call.respondJson(
                body =
                successResponse(
                    "/tickets/$id/attachments",
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

private val allowedTicketCategories = setOf("BUG", "ACCESS", "BILLING", "CHANGE_REQUEST", "QUESTION", "OTHER")
private val allowedPlatforms = setOf("ANDROID", "IOS", "DESKTOP", "WEB", "BACKEND", "OTHER")
private val allowedPriorities = setOf("LOW", "MEDIUM", "HIGH", "URGENT")
private val allowedStatuses = setOf("OPEN", "IN_PROGRESS", "PENDING_CLIENT", "RESOLVED", "CLOSED")
