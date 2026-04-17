package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.utils.adminOwnerId
import com.requena.supportdesk.server.utils.clientJson
import com.requena.supportdesk.server.utils.clientsJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.messageJson
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

fun Route.clientRoutes(service: SupportDeskService) {
    route("/admin") {
        get("/clients") {
            call.respondJson(body = successResponse("/admin/clients", clientsJson(service.clients(call.adminOwnerId()))))
        }
        post("/clients") {
            val ownerAdminId = call.adminOwnerId()
            val request = call.receiveOrDefault(CreateClientRequest())
            if (
                request.companyName.isBlank() ||
                request.productName.isBlank() ||
                request.contactName.isBlank() ||
                request.email.isBlank() ||
                request.accountStatus !in allowedClientStatuses ||
                request.serviceTier !in allowedServiceTiers ||
                request.preferredContactChannel !in allowedContactChannels
            ) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client payload"))
            }
            call.respondJson(
                HttpStatusCode.Created,
                successResponse("/admin/clients", clientJson(service.createdClient(request, ownerAdminId))),
            )
        }
        patch("/clients/{clientId}") {
            val ownerAdminId = call.adminOwnerId()
            val clientId = call.parameters["clientId"].orEmpty()
            val request = call.receiveOrDefault(UpdateClientRequest())
            if (clientId.isBlank()) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Client id is required"))
            }
            if (
                request.accountStatus?.let { it !in allowedClientStatuses } == true ||
                request.serviceTier?.let { it !in allowedServiceTiers } == true ||
                request.preferredContactChannel?.let { it !in allowedContactChannels } == true
            ) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client payload"))
            }
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId",
                    clientJson(service.updatedClient(clientId, request, ownerAdminId)),
                ),
            )
        }
        delete("/clients/{clientId}") {
            val ownerAdminId = call.adminOwnerId()
            val clientId = call.parameters["clientId"].orEmpty()
            if (clientId.isBlank()) {
                return@delete call.respondJson(HttpStatusCode.BadRequest, errorResponse("Client id is required"))
            }
            service.deletedClient(clientId, ownerAdminId)
            call.respondJson(body = successResponse("/admin/clients/$clientId", messageJson("Client deleted")))
        }
    }
}

private val allowedClientStatuses = setOf("ACTIVE", "PAUSED", "INACTIVE")
private val allowedServiceTiers = setOf("STANDARD", "PRIORITY", "VIP")
private val allowedContactChannels = setOf("TICKET", "EMAIL", "WHATSAPP", "CALL")
