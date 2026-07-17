package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.ApproveClientProgramRequest
import com.requena.supportdesk.server.domain.model.CreateClientProgramRequestsRequest
import com.requena.supportdesk.server.domain.model.RejectClientProgramRequest
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.plugins.SensitiveOperationRateLimit
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.clientBillingPreviewJson
import com.requena.supportdesk.server.utils.clientProgramRequestJson
import com.requena.supportdesk.server.utils.clientProgramRequestsJson
import com.requena.supportdesk.server.utils.clientProgramsJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.isAdmin
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private val programRequestStatuses = setOf("REQUESTED", "APPROVED", "REJECTED", "CANCELLED")

fun Route.programRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/client") {
        get("/programs") {
            val identity = call.requireClientProgramIdentity(tokenService) ?: return@get
            call.respondJson(
                body = successResponse(
                    "/client/programs",
                    clientProgramsJson(service.clientPrograms(requireNotNull(identity.clientId))),
                ),
            )
        }
        rateLimit(SensitiveOperationRateLimit) {
            post("/program-requests") {
                val identity = call.requireClientProgramIdentity(tokenService) ?: return@post
                val request = call.receiveOrDefault(CreateClientProgramRequestsRequest())
                val normalizedKeys = request.productKeys.map(String::trim).filter(String::isNotBlank)
                if (
                    normalizedKeys.isEmpty() ||
                    normalizedKeys.size > 8 ||
                    normalizedKeys.distinct().size != normalizedKeys.size ||
                    request.customerNote.length > 500
                ) {
                    return@post call.respondJson(
                        HttpStatusCode.BadRequest,
                        errorResponse("Invalid program request payload"),
                    )
                }
                val created = service.createdClientProgramRequests(
                    clientId = requireNotNull(identity.clientId),
                    requestedByUserId = identity.userId,
                    request = request.copy(productKeys = normalizedKeys),
                )
                call.respondJson(
                    HttpStatusCode.Created,
                    successResponse(
                        "/client/program-requests",
                        clientProgramRequestsJson(created, includeAdminContext = false),
                    ),
                )
            }
        }
    }

    route("/admin") {
        get("/program-requests") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val status = call.request.queryParameters["status"]?.trim()?.uppercase()
            if (status != null && status !in programRequestStatuses) {
                return@get call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid program request status"))
            }
            call.respondJson(
                body = successResponse(
                    "/admin/program-requests",
                    clientProgramRequestsJson(service.clientProgramRequests(identity.userId, status), includeAdminContext = true),
                ),
            )
        }
        get("/clients/{clientId}/billing-preview") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@get
            val clientId = call.parameters["clientId"].orEmpty()
            val period = call.request.queryParameters["period"].orEmpty()
            if (clientId.isBlank() || period.isBlank()) {
                return@get call.respondJson(
                    HttpStatusCode.BadRequest,
                    errorResponse("Client id and YYYY-MM billing period are required"),
                )
            }
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/billing-preview",
                    clientBillingPreviewJson(service.clientBillingPreview(clientId, period, identity.userId)),
                ),
            )
        }
        rateLimit(SensitiveOperationRateLimit) {
            post("/program-requests/{requestId}/approve") {
                val identity = call.requireAdminIdentity(tokenService) ?: return@post
                val requestId = call.parameters["requestId"].orEmpty()
                val request = call.receiveOrDefault(ApproveClientProgramRequest())
                if (
                    requestId.isBlank() ||
                    request.monthlyPriceCents != 0L ||
                    (request.adminNote?.length ?: 0) > 500
                ) {
                    return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid program approval payload"))
                }
                val approved = service.approvedClientProgramRequest(
                    requestId = requestId,
                    request = request,
                    reviewedByUserId = identity.userId,
                    ownerAdminId = identity.userId,
                )
                call.respondJson(
                    body = successResponse(
                        "/admin/program-requests/$requestId/approve",
                        clientProgramRequestJson(approved, includeAdminContext = true),
                    ),
                )
            }
            post("/program-requests/{requestId}/reject") {
                val identity = call.requireAdminIdentity(tokenService) ?: return@post
                val requestId = call.parameters["requestId"].orEmpty()
                val request = call.receiveOrDefault(RejectClientProgramRequest())
                if (requestId.isBlank() || (request.adminNote?.length ?: 0) > 500) {
                    return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid program rejection payload"))
                }
                val rejected = service.rejectedClientProgramRequest(
                    requestId = requestId,
                    request = request,
                    reviewedByUserId = identity.userId,
                    ownerAdminId = identity.userId,
                )
                call.respondJson(
                    body = successResponse(
                        "/admin/program-requests/$requestId/reject",
                        clientProgramRequestJson(rejected, includeAdminContext = true),
                    ),
                )
            }
        }
    }
}

private suspend fun ApplicationCall.requireClientProgramIdentity(
    tokenService: SupportDeskTokenService,
): ServerAuthIdentity? {
    val identity = requireAuthenticatedIdentity(tokenService) ?: return null
    if (!identity.isAdmin && identity.clientId != null) return identity
    respondJson(HttpStatusCode.Forbidden, errorResponse("Client portal access is required"))
    return null
}
