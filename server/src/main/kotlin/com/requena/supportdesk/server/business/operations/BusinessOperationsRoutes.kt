package com.requena.supportdesk.server.business.operations

import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.receiveOrDefault
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate

/**
 * Adapter point for the existing subscription/entitlement and client membership systems.
 * It must verify the entitlement server-side and return a tenant-scoped role; request body data is never trusted for it.
 */
fun interface OperationsEntitlementGuard {
    suspend fun authorize(identity: ServerAuthIdentity, productKey: String): OperationsPrincipal?
}

@Serializable private data class AppointmentPayload(val serviceId: String = "", val resourceId: String = "", val startsAt: String = "", val endsAt: String = "", val timeZone: String = "", val contactName: String? = null, val contactEmail: String? = null, val contactPhone: String? = null, val notes: String? = null)
@Serializable private data class CancelPayload(val reason: String? = null)
@Serializable private data class BookingServicePayload(val name: String = "", val durationMinutes: Int = 0)
@Serializable private data class BookingResourcePayload(val name: String = "", val timeZone: String = "")
@Serializable private data class AvailabilityRulePayload(val resourceId: String = "", val weekday: Int = 0, val startsAt: String = "", val endsAt: String = "", val timeZone: String = "")
@Serializable private data class DocumentPayload(val title: String = "", val folderId: String? = null)
@Serializable private data class FolderPayload(val name: String = "", val parentFolderId: String? = null)
@Serializable private data class UploadPayload(val fileName: String = "", val contentType: String = "", val sizeBytes: Long = 0)
@Serializable private data class ConfirmationRequestPayload(val documentVersionId: String = "", val title: String = "", val statement: String = "", val expiresAt: String? = null)

/** Public routes, deliberately not registered in the main module until the integrator supplies [guard], storage and scanner. */
fun Route.businessOperationsRoutes(
    service: BusinessOperationsService,
    tokenService: SupportDeskTokenService,
    guard: OperationsEntitlementGuard,
) {
    route("/client/business") {
        get("/bookings/configuration") {
            call.operationResponse("/client/business/bookings/configuration") {
                val principal = call.principal(tokenService, guard, BOOKINGS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val (services, resources) = service.bookingConfiguration(principal)
                buildJsonObject { put("services", buildJsonArray { services.forEach { add(bookingServiceJson(it)) } }); put("resources", buildJsonArray { resources.forEach { add(bookingResourceJson(it)) } }) }
            }
        }
        post("/bookings/services") {
            call.operationResponse("/client/business/bookings/services", HttpStatusCode.Created) {
                val principal = call.principal(tokenService, guard, BOOKINGS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val payload = call.receiveOrDefault(BookingServicePayload())
                bookingServiceJson(service.createBookingService(principal, CreateBookingServiceCommand(payload.name, payload.durationMinutes)))
            }
        }
        post("/bookings/resources") {
            call.operationResponse("/client/business/bookings/resources", HttpStatusCode.Created) {
                val principal = call.principal(tokenService, guard, BOOKINGS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val payload = call.receiveOrDefault(BookingResourcePayload())
                bookingResourceJson(service.createBookingResource(principal, CreateBookingResourceCommand(payload.name, payload.timeZone)))
            }
        }
        post("/bookings/availability-rules") {
            call.operationResponse("/client/business/bookings/availability-rules", HttpStatusCode.Created) {
                val principal = call.principal(tokenService, guard, BOOKINGS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val payload = call.receiveOrDefault(AvailabilityRulePayload())
                availabilityRuleJson(service.createAvailabilityRule(principal, CreateAvailabilityRuleCommand(payload.resourceId, payload.weekday, payload.startsAt, payload.endsAt, payload.timeZone)))
            }
        }
        get("/bookings/appointments") {
            call.operationResponse("/client/business/bookings/appointments") {
                val principal = call.principal(tokenService, guard, BOOKINGS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val from = call.instantParameter("from"); val to = call.instantParameter("to")
                appointmentsJson(service.agenda(principal, from, to))
            }
        }
        get("/bookings/availability") {
            call.operationResponse("/client/business/bookings/availability") {
                val principal = call.principal(tokenService, guard, BOOKINGS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val serviceId = call.request.queryParameters["serviceId"].orEmpty()
                val resourceId = call.request.queryParameters["resourceId"].orEmpty()
                val date = runCatching { LocalDate.parse(call.request.queryParameters["date"].orEmpty()) }.getOrElse { throw BusinessOperationsValidationException("date must be ISO-8601") }
                slotsJson(service.availability(principal, serviceId, resourceId, date))
            }
        }
        post("/bookings/appointments") {
            call.operationResponse("/client/business/bookings/appointments", HttpStatusCode.Created) {
                val principal = call.principal(tokenService, guard, BOOKINGS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val payload = call.receiveOrDefault(AppointmentPayload())
                appointmentJson(service.createAppointment(principal, payload.toCommand()))
            }
        }
        post("/bookings/appointments/{appointmentId}/cancel") {
            call.operationResponse("/client/business/bookings/appointments/cancel") {
                val principal = call.principal(tokenService, guard, BOOKINGS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                appointmentJson(service.cancelAppointment(principal, call.parameters["appointmentId"].orEmpty(), call.receiveOrDefault(CancelPayload()).reason))
            }
        }

        get("/documents") {
            call.operationResponse("/client/business/documents") {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                documentsJson(service.listDocuments(principal))
            }
        }
        get("/documents/folders") {
            call.operationResponse("/client/business/documents/folders") {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                buildJsonArray { service.listFolders(principal).forEach { add(folderJson(it)) } }
            }
        }
        post("/documents/folders") {
            call.operationResponse("/client/business/documents/folders", HttpStatusCode.Created) {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val payload = call.receiveOrDefault(FolderPayload())
                folderJson(service.createFolder(principal, payload.name, payload.parentFolderId))
            }
        }
        post("/documents") {
            call.operationResponse("/client/business/documents", HttpStatusCode.Created) {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                documentJson(service.createDocument(principal, call.receiveOrDefault(DocumentPayload()).let { CreateDocumentCommand(it.title, it.folderId) }))
            }
        }
        post("/documents/{documentId}/versions/upload-intents") {
            call.operationResponse("/client/business/documents/upload-intents", HttpStatusCode.Created) {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val payload = call.receiveOrDefault(UploadPayload())
                uploadIntentJson(service.prepareUpload(principal, call.parameters["documentId"].orEmpty(), PrepareDocumentUploadCommand(payload.fileName, payload.contentType, payload.sizeBytes)))
            }
        }
        post("/documents/versions/{versionId}/complete") {
            call.operationResponse("/client/business/documents/versions/complete") {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                documentVersionJson(service.completeUpload(principal, call.parameters["versionId"].orEmpty()))
            }
        }
        get("/documents/versions/{versionId}/download") {
            call.operationResponse("/client/business/documents/versions/download") {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                buildJsonObject { put("downloadUrl", service.issueDownload(principal, call.parameters["versionId"].orEmpty())) }
            }
        }
        post("/documents/confirmation-requests") {
            call.operationResponse("/client/business/documents/confirmation-requests", HttpStatusCode.Created) {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                val payload = call.receiveOrDefault(ConfirmationRequestPayload())
                confirmationRequestJson(service.createConfirmationRequest(principal, CreateConfirmationRequestCommand(payload.documentVersionId, payload.title, payload.statement, payload.expiresAt?.let(::parseInstant))))
            }
        }
        post("/documents/confirmation-requests/{requestId}/accept") {
            call.operationResponse("/client/business/documents/confirmation-requests/accept") {
                val principal = call.principal(tokenService, guard, DOCUMENTS_PRODUCT_KEY) ?: return@operationResponse JsonNull
                // Network evidence is intentionally left null until a trusted reverse-proxy adapter is supplied.
                confirmationJson(service.acceptConfirmation(principal, call.parameters["requestId"].orEmpty(), principal.email, ConfirmationEvidence()))
            }
        }
    }
}

private suspend fun ApplicationCall.principal(tokenService: SupportDeskTokenService, guard: OperationsEntitlementGuard, productKey: String): OperationsPrincipal? {
    val identity = requireAuthenticatedIdentity(tokenService) ?: return null
    return guard.authorize(identity, productKey)?.copy(userId = identity.userId, email = identity.email) ?: run {
        respondJson(HttpStatusCode.Forbidden, errorResponse("An active entitlement and membership are required")); null
    }
}

private suspend fun ApplicationCall.operationResponse(path: String, success: HttpStatusCode = HttpStatusCode.OK, block: suspend () -> JsonElement) {
    try {
        val data = block()
        if (data !== JsonNull) respondJson(success, successResponse(path, data))
    } catch (error: BusinessOperationsValidationException) { respondJson(HttpStatusCode.BadRequest, errorResponse(error.message ?: "Invalid request"))
    } catch (error: BusinessOperationsForbiddenException) { respondJson(HttpStatusCode.Forbidden, errorResponse("Forbidden"))
    } catch (error: BusinessOperationsNotFoundException) { respondJson(HttpStatusCode.NotFound, errorResponse("Resource not found"))
    } catch (error: BusinessOperationsConflictException) { respondJson(HttpStatusCode.Conflict, errorResponse(error.message ?: "Conflict"))
    } catch (error: IllegalStateException) { respondJson(HttpStatusCode.Conflict, errorResponse("Operation could not be completed")) }
}

private fun AppointmentPayload.toCommand() = CreateAppointmentCommand(serviceId, resourceId, parseInstant(startsAt), parseInstant(endsAt), timeZone, contactName, contactEmail, contactPhone, notes)
private fun parseInstant(value: String): Instant = runCatching { Instant.parse(value) }.getOrElse { throw BusinessOperationsValidationException("Timestamp must be ISO-8601 UTC") }
private fun ApplicationCall.instantParameter(name: String): Instant = request.queryParameters[name]?.let(::parseInstant) ?: throw BusinessOperationsValidationException("$name is required")

private fun appointmentsJson(items: List<Appointment>) = buildJsonArray { items.forEach { add(appointmentJson(it)) } }
private fun bookingServiceJson(item: BookingService) = buildJsonObject { put("id",item.id);put("name",item.name);put("durationMinutes",item.durationMinutes);put("active",item.active) }
private fun bookingResourceJson(item: BookingResource) = buildJsonObject { put("id",item.id);put("name",item.name);put("timeZone",item.timeZone);put("active",item.active) }
private fun availabilityRuleJson(item: AvailabilityRule) = buildJsonObject { put("id",item.id);put("resourceId",item.resourceId);put("weekday",item.weekday);put("startsAt",item.startsAt.toString());put("endsAt",item.endsAt.toString());put("timeZone",item.timeZone) }
private fun appointmentJson(item: Appointment) = buildJsonObject { put("id",item.id);put("serviceId",item.serviceId);put("resourceId",item.resourceId);put("startsAt",item.startsAt.toString());put("endsAt",item.endsAt.toString());put("timeZone",item.timeZone);put("status",item.status.name);putNullable("contactName",item.contactName);putNullable("contactEmail",item.contactEmail);putNullable("contactPhone",item.contactPhone);putNullable("notes",item.notes);put("createdAt",item.createdAt.toString()) }
private fun slotsJson(items: List<AvailableSlot>) = buildJsonArray { items.forEach { add(buildJsonObject { put("startsAt",it.startsAt.toString());put("endsAt",it.endsAt.toString());put("timeZone",it.timeZone) }) } }
private fun documentsJson(items: List<BusinessDocument>) = buildJsonArray { items.forEach { add(documentJson(it)) } }
private fun documentJson(item: BusinessDocument) = buildJsonObject { put("id",item.id);putNullable("folderId",item.folderId);put("title",item.title);put("status",item.status.name);put("createdAt",item.createdAt.toString());put("updatedAt",item.updatedAt.toString()) }
private fun folderJson(item: DocumentFolder) = buildJsonObject { put("id",item.id);put("name",item.name);putNullable("parentFolderId",item.parentFolderId);put("createdAt",item.createdAt.toString()) }
private fun uploadIntentJson(item: UploadIntent) = buildJsonObject { put("versionId",item.versionId);put("uploadUrl",item.uploadUrl);put("expiresAt",item.expiresAt.toString()) }
private fun documentVersionJson(item: DocumentVersion) = buildJsonObject { put("id",item.id);put("documentId",item.documentId);put("versionNumber",item.versionNumber);put("fileName",item.fileName);put("contentType",item.contentType);put("sizeBytes",item.sizeBytes);putNullable("sha256",item.sha256);put("scanStatus",item.scanStatus.name);put("createdAt",item.createdAt.toString()) }
private fun confirmationRequestJson(item: ConfirmationRequest) = buildJsonObject { put("id",item.id);put("documentVersionId",item.documentVersionId);put("title",item.title);put("statement",item.statement);put("status",item.status.name);putNullable("expiresAt",item.expiresAt?.toString()) }
private fun confirmationJson(item: DocumentConfirmation) = buildJsonObject { put("id",item.id);put("confirmationRequestId",item.confirmationRequestId);put("documentVersionId",item.documentVersionId);put("acceptedAt",item.acceptedAt.toString());put("documentSha256",item.documentSha256);put("evidenceType","AUTHENTICATED_CONFIRMATION") }
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) { if (value == null) put(key, JsonNull) else put(key, value) }
