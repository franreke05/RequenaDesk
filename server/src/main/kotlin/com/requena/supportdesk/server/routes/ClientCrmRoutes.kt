package com.requena.supportdesk.server.routes

import com.requena.supportdesk.server.domain.model.CreateClientActivityRequest
import com.requena.supportdesk.server.domain.model.CreateClientContactRequest
import com.requena.supportdesk.server.domain.model.UpdateClientActivityRequest
import com.requena.supportdesk.server.domain.model.UpdateClientContactRequest
import com.requena.supportdesk.server.domain.service.SupportDeskService
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.clientActivitiesJson
import com.requena.supportdesk.server.utils.clientActivityJson
import com.requena.supportdesk.server.utils.clientContactJson
import com.requena.supportdesk.server.utils.clientContactsJson
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.messageJson
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.requireAdminIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDate

private val allowedClientActivityTypes = setOf("CALL", "EMAIL", "MEETING", "FOLLOW_UP", "NOTE")

fun Route.clientCrmRoutes(service: SupportDeskService, tokenService: SupportDeskTokenService) {
    route("/admin/clients/{clientId}") {
        get("/contacts") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@get
            val clientId = call.clientIdOrRespond() ?: return@get
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/contacts",
                    clientContactsJson(service.clientContacts(clientId, ownerAdminId)),
                ),
            )
        }
        post("/contacts") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@post
            val clientId = call.clientIdOrRespond() ?: return@post
            val request = call.receiveOrDefault(CreateClientContactRequest())
            if (!request.isValidForCreation()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client contact payload"))
            }
            call.respondJson(
                status = HttpStatusCode.Created,
                body = successResponse(
                    "/admin/clients/$clientId/contacts",
                    clientContactJson(service.createdClientContact(clientId, request, ownerAdminId)),
                ),
            )
        }
        patch("/contacts/{contactId}") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@patch
            val clientId = call.clientIdOrRespond() ?: return@patch
            val contactId = call.parameters["contactId"].orEmpty()
            val request = call.receiveOrDefault(UpdateClientContactRequest())
            if (contactId.isBlank() || !request.isValidForUpdate()) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client contact payload"))
            }
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/contacts/$contactId",
                    clientContactJson(service.updatedClientContact(clientId, contactId, request, ownerAdminId)),
                ),
            )
        }
        delete("/contacts/{contactId}") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@delete
            val clientId = call.clientIdOrRespond() ?: return@delete
            val contactId = call.parameters["contactId"].orEmpty()
            if (contactId.isBlank()) {
                return@delete call.respondJson(HttpStatusCode.BadRequest, errorResponse("Contact id is required"))
            }
            service.deletedClientContact(clientId, contactId, ownerAdminId)
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/contacts/$contactId",
                    messageJson("Client contact deleted"),
                ),
            )
        }
        get("/activities") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@get
            val clientId = call.clientIdOrRespond() ?: return@get
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/activities",
                    clientActivitiesJson(service.clientActivities(clientId, ownerAdminId)),
                ),
            )
        }
        post("/activities") {
            val identity = call.requireAdminIdentity(tokenService) ?: return@post
            val clientId = call.clientIdOrRespond() ?: return@post
            val request = call.receiveOrDefault(CreateClientActivityRequest())
            if (!request.isValidForCreation()) {
                return@post call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client activity payload"))
            }
            call.respondJson(
                status = HttpStatusCode.Created,
                body = successResponse(
                    "/admin/clients/$clientId/activities",
                    clientActivityJson(
                        service.createdClientActivity(
                            clientId = clientId,
                            request = request,
                            createdById = identity.userId,
                            ownerAdminId = identity.userId,
                        ),
                    ),
                ),
            )
        }
        patch("/activities/{activityId}") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@patch
            val clientId = call.clientIdOrRespond() ?: return@patch
            val activityId = call.parameters["activityId"].orEmpty()
            val request = call.receiveOrDefault(UpdateClientActivityRequest())
            if (activityId.isBlank() || !request.isValidForUpdate()) {
                return@patch call.respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid client activity payload"))
            }
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/activities/$activityId",
                    clientActivityJson(service.updatedClientActivity(clientId, activityId, request, ownerAdminId)),
                ),
            )
        }
        delete("/activities/{activityId}") {
            val ownerAdminId = call.requireAdminIdentity(tokenService)?.userId ?: return@delete
            val clientId = call.clientIdOrRespond() ?: return@delete
            val activityId = call.parameters["activityId"].orEmpty()
            if (activityId.isBlank()) {
                return@delete call.respondJson(HttpStatusCode.BadRequest, errorResponse("Activity id is required"))
            }
            service.deletedClientActivity(clientId, activityId, ownerAdminId)
            call.respondJson(
                body = successResponse(
                    "/admin/clients/$clientId/activities/$activityId",
                    messageJson("Client activity deleted"),
                ),
            )
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.clientIdOrRespond(): String? {
    val clientId = parameters["clientId"].orEmpty()
    if (clientId.isNotBlank()) return clientId
    respondJson(HttpStatusCode.BadRequest, errorResponse("Client id is required"))
    return null
}

private fun CreateClientContactRequest.isValidForCreation(): Boolean =
    fullName.trim().length in 1..180 &&
        email.isValidOptionalEmail() &&
        phone.isValidOptionalLength(80) &&
        role.isValidOptionalLength(120)

private fun UpdateClientContactRequest.isValidForUpdate(): Boolean =
    (fullName == null || fullName.trim().length in 1..180) &&
        email.isValidOptionalEmail() &&
        phone.isValidOptionalLength(80) &&
        role.isValidOptionalLength(120)

private fun CreateClientActivityRequest.isValidForCreation(): Boolean =
    type in allowedClientActivityTypes &&
        subject.trim().length in 1..220 &&
        details.isValidOptionalLength(10_000) &&
        dueDate.isValidOptionalDate()

private fun UpdateClientActivityRequest.isValidForUpdate(): Boolean =
    (type == null || type in allowedClientActivityTypes) &&
        (subject == null || subject.trim().length in 1..220) &&
        details.isValidOptionalLength(10_000) &&
        dueDate.isValidOptionalDate()

private fun String?.isValidOptionalEmail(): Boolean =
    isNullOrBlank() || (length <= 254 && contains("@"))

private fun String?.isValidOptionalLength(maxLength: Int): Boolean =
    isNullOrBlank() || length <= maxLength

private fun String?.isValidOptionalDate(): Boolean =
    isNullOrBlank() || runCatching { LocalDate.parse(trim()) }.isSuccess
