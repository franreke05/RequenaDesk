package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.service.SupportDeskPlaceholderService
import com.requena.supportdesk.server.utils.clientJson
import com.requena.supportdesk.server.utils.clientsJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.clientRoutes(service: SupportDeskPlaceholderService) {
    route("/admin") {
        get("/clients") {
            call.respond(successResponse("/admin/clients", clientsJson(service.clients())))
        }
        post("/clients") {
            val request = runCatching { call.receive<CreateClientRequest>() }.getOrDefault(CreateClientRequest())
            if (request.accountStatus !in allowedClientStatuses || request.serviceTier !in allowedServiceTiers || request.preferredContactChannel !in allowedContactChannels) {
                return@post call.respond(HttpStatusCode.BadRequest, errorResponse("Invalid client payload"))
            }
            call.respond(HttpStatusCode.Created, successResponse("/admin/clients", clientJson(service.createdClient(request))))
        }
    }
}

private val allowedClientStatuses = setOf("ACTIVE", "PAUSED", "INACTIVE")
private val allowedServiceTiers = setOf("STANDARD", "PRIORITY", "VIP")
private val allowedContactChannels = setOf("TICKET", "EMAIL", "WHATSAPP", "CALL")
