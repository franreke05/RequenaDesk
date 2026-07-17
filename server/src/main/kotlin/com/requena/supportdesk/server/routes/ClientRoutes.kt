package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientCredentialsRequest
import com.requena.supportdesk.server.domain.model.UpdateClientComponentsRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.clientCredentialsJson
import com.requena.supportdesk.server.utils.clientJson
import com.requena.supportdesk.server.utils.clientProvisioningJson
import com.requena.supportdesk.server.utils.clientsJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.isAdmin
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.clientRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin") {
        get("/clients") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            val clients = if (identity.isAdmin) {
                service.clients(identity.userId)
            } else {
                service.clients().filter { it.id == identity.clientId }
            }
            call.respondJson(body = successResponse("/admin/clients", clientsJson(clients)))
        }
        post("/clients") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@post
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
                successResponse("/admin/clients", clientProvisioningJson(service.createdClient(request, ownerAdminId))),
            )
        }
        patch("/clients/{clientId}") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@patch
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
        post("/clients/{clientId}/credentials") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@post
            val clientId = call.parameters["clientId"].orEmpty()
            val request = call.receiveOrDefault(UpdateClientCredentialsRequest())
            if (
                clientId.isBlank() ||
                request.email.isBlank() ||
                !request.email.contains("@") ||
                request.password.length < 8
            ) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client credentials payload"))
            }
            service.updatedClientCredentials(clientId, request, ownerAdminId)
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/credentials",
                    messageJson("Client credentials updated"),
                ),
            )
        }
        post("/clients/{clientId}/credentials/regenerate") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@post
            val clientId = call.parameters["clientId"].orEmpty()
            if (clientId.isBlank()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Client id is required"))
            }
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/credentials/regenerate",
                    clientCredentialsJson(service.regeneratedClientCredentials(clientId, ownerAdminId)),
                ),
            )
        }
        put("/clients/{clientId}/components") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@put
            val clientId = call.parameters["clientId"].orEmpty()
            val request = call.receiveOrDefault(UpdateClientComponentsRequest())
            if (clientId.isBlank() || request.components.any { it !in allowedClientComponents }) {
                return@put call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client components payload"))
            }
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/components",
                    clientJson(service.updatedClientComponents(clientId, request, ownerAdminId)),
                ),
            )
        }
        delete("/clients/{clientId}") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@delete
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
private val allowedClientComponents = setOf("SERVICE_SLA")
