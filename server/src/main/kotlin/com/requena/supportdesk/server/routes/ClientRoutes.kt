package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.utils.orgId

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientCredentialsRequest
import com.requena.supportdesk.server.domain.model.UpdateClientComponentsRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.plugins.SensitiveOperationRateLimit
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
import io.ktor.server.plugins.ratelimit.rateLimit

fun Route.clientRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService, plans: com.requena.supportdesk.server.domain.service.OrganizationPlanService? = null) {
    route("/admin") {
        get("/clients") {
            val identity = call.requireAuthenticatedIdentity(tokenService) ?: return@get
            val clients = if (identity.isAdmin) {
                service.clients(identity.orgId)
            } else {
                service.clients(identity.orgId).filter { it.id == identity.clientId }
            }
            call.respondJson(body = successResponse("/admin/clients", clientsJson(clients)))
        }
        post("/clients") {
            val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@post
            val received = call.receiveOrDefault(CreateClientRequest())
            val request = received.copy(billing = received.billing.normalized())
            request.billing.problem()?.let { problem ->
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse(problem))
            }
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
            plans?.ensureClientCapacity(organizationId)
            call.respondJson(
                HttpStatusCode.Created,
                successResponse("/admin/clients", clientProvisioningJson(service.createdClient(request, organizationId))),
            )
        }
        patch("/clients/{clientId}") {
            val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@patch
            val clientId = call.parameters["clientId"].orEmpty()
            val received = call.receiveOrDefault(UpdateClientRequest())
            val request = received.copy(billing = received.billing?.normalized())
            if (clientId.isBlank()) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Client id is required"))
            }
            request.billing?.problem()?.let { problem ->
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse(problem))
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
                    clientJson(service.updatedClient(clientId, request, organizationId)),
                ),
            )
        }
        rateLimit(SensitiveOperationRateLimit) {
            post("/clients/{clientId}/credentials") {
                val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@post
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
                service.updatedClientCredentials(clientId, request, organizationId)
                call.respondJson(
                    body = successResponse(
                        "/admin/clients/$clientId/credentials",
                        messageJson("Client credentials updated"),
                    ),
                )
            }
            post("/clients/{clientId}/credentials/regenerate") {
                val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@post
                val clientId = call.parameters["clientId"].orEmpty()
                if (clientId.isBlank()) {
                    return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Client id is required"))
                }
                call.respondJson(
                    body = successResponse(
                        "/admin/clients/$clientId/credentials/regenerate",
                        clientCredentialsJson(service.regeneratedClientCredentials(clientId, organizationId)),
                    ),
                )
            }
        }
        put("/clients/{clientId}/components") {
            val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@put
            val clientId = call.parameters["clientId"].orEmpty()
            val request = call.receiveOrDefault(UpdateClientComponentsRequest())
            if (clientId.isBlank() || request.components.any { it !in allowedClientComponents }) {
                return@put call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client components payload"))
            }
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/components",
                    clientJson(service.updatedClientComponents(clientId, request, organizationId)),
                ),
            )
        }
        delete("/clients/{clientId}") {
            val organizationId = call.requireAdminIdentity(tokenService)?.orgId ?: return@delete
            val clientId = call.parameters["clientId"].orEmpty()
            if (clientId.isBlank()) {
                return@delete call.respondJson(HttpStatusCode.BadRequest, errorResponse("Client id is required"))
            }
            service.deletedClient(clientId, organizationId)
            call.respondJson(body = successResponse("/admin/clients/$clientId", messageJson("Client deleted")))
        }
    }
}

private val allowedClientStatuses = setOf("ACTIVE", "PAUSED", "INACTIVE")
private val allowedServiceTiers = setOf("STANDARD", "PRIORITY", "VIP")
private val allowedContactChannels = setOf("TICKET", "EMAIL", "WHATSAPP", "CALL")
private val allowedClientComponents = setOf("SERVICE_SLA")
