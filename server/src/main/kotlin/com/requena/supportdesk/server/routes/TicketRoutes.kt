package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.service.SupportDeskPlaceholderService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.successResponse
import com.requena.supportdesk.server.utils.ticketJson
import com.requena.supportdesk.server.utils.ticketsJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Route.ticketRoutes(service: SupportDeskPlaceholderService) {
    route("/tickets") {
        get {
            call.respond(successResponse("/tickets", ticketsJson(service.tickets())))
        }
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val ticket = service.ticket(id) ?: return@get call.respond(HttpStatusCode.NotFound, errorResponse("Ticket not found"))
            call.respond(successResponse("/tickets/$id", ticketJson(ticket)))
        }
        post {
            val request = runCatching { call.receive<CreateTicketRequest>() }.getOrDefault(CreateTicketRequest())
            if (request.category !in allowedTicketCategories || request.platform !in allowedPlatforms || request.priority !in allowedPriorities) {
                return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Invalid ticket payload"))
            }
            call.respond(HttpStatusCode.Created, successResponse("/tickets", ticketJson(service.createdTicket(request))))
        }
        post("/{id}/messages") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val result = service.createdMessage(id)
            call.respond(
                successResponse(
                    "/tickets/$id/messages",
                    buildJsonObject {
                        put("ticketId", result.first)
                        put("message", result.second)
                    },
                ),
            )
        }
        patch("/{id}/status") {
            val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val request = runCatching { call.receive<UpdateTicketStatusRequest>() }.getOrDefault(UpdateTicketStatusRequest())
            if (request.status !in allowedStatuses) {
                return@patch call.respond(HttpStatusCode.BadRequest, errorResponse("Invalid ticket status"))
            }
            val result = service.updatedStatus(id, request)
            call.respond(
                successResponse(
                    "/tickets/$id/status",
                    buildJsonObject {
                        put("ticketId", result.first)
                        put("status", result.second)
                    },
                ),
            )
        }
        patch("/{id}/priority") {
            val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val request = runCatching { call.receive<UpdateTicketPriorityRequest>() }.getOrDefault(UpdateTicketPriorityRequest())
            if (request.priority !in allowedPriorities) {
                return@patch call.respond(HttpStatusCode.BadRequest, errorResponse("Invalid ticket priority"))
            }
            val result = service.updatedPriority(id, request)
            call.respond(
                successResponse(
                    "/tickets/$id/priority",
                    buildJsonObject {
                        put("ticketId", result.first)
                        put("priority", result.second)
                    },
                ),
            )
        }
        post("/{id}/attachments") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Missing ticket id"))
            val result = service.uploadedAttachment(id)
            call.respond(
                successResponse(
                    "/tickets/$id/attachments",
                    buildJsonObject {
                        put("ticketId", result.first)
                        put("attachmentId", result.second)
                        put("message", "Upload placeholder queued")
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
