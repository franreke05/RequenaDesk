package com.requena.supportdesk.server.business.finance

import com.requena.supportdesk.core.network.ApiEnvelope
import com.requena.supportdesk.core.network.ApiErrorEnvelope
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceValidationException
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

typealias BusinessFinanceIdentityResolver = suspend (ApplicationCall) -> BusinessFinanceClientIdentity?

/**
 * Registration is intentionally delegated to SupportDeskModule. The resolver
 * must bridge the existing JWT identity and reject admin/non-client sessions.
 */
fun Route.businessFinanceRoutes(
    service: BusinessFinanceService,
    resolveIdentity: BusinessFinanceIdentityResolver,
) {
    route("/client/business") {
        route("/invoicing") {
            get("/documents") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@get
                call.respondFinance("/client/business/invoicing/documents") { service.listSalesDocuments(identity) }
            }
            post("/documents") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@post
                if (!call.requireIdempotencyKey()) return@post
                val request = call.receive<SalesDocumentDraftInput>()
                call.respondFinance("/client/business/invoicing/documents", HttpStatusCode.Created) {
                    service.createSalesDraft(identity, request)
                }
            }
            patch("/documents/{documentId}") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@patch
                if (!call.requireIdempotencyKey()) return@patch
                val version = call.expectedVersion() ?: return@patch
                val documentId = call.parameters["documentId"].orEmpty()
                if (!documentId.isUuid()) return@patch call.respondFinanceError(HttpStatusCode.BadRequest, "Document id must be a UUID")
                val request = call.receive<SalesDocumentDraftInput>()
                call.respondFinance("/client/business/invoicing/documents/$documentId") {
                    service.updateSalesDraft(identity, documentId, version, request)
                }
            }
            post("/documents/{documentId}/archive") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@post
                if (!call.requireIdempotencyKey()) return@post
                val version = call.expectedVersion() ?: return@post
                val documentId = call.parameters["documentId"].orEmpty()
                if (!documentId.isUuid()) return@post call.respondFinanceError(HttpStatusCode.BadRequest, "Document id must be a UUID")
                call.respondFinance("/client/business/invoicing/documents/$documentId/archive") {
                    service.archiveSalesDocument(identity, documentId, version)
                }
            }
        }
        route("/accounting") {
            get("/entries") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@get
                call.respondFinance("/client/business/accounting/entries") { service.listFinanceEntries(identity) }
            }
            get("/overview") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@get
                val period = call.request.queryParameters["period"].orEmpty()
                call.respondFinance("/client/business/accounting/overview") { service.financeOverview(identity, period) }
            }
            post("/entries") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@post
                if (!call.requireIdempotencyKey()) return@post
                val request = call.receive<FinanceEntryInput>()
                call.respondFinance("/client/business/accounting/entries", HttpStatusCode.Created) {
                    service.createFinanceEntry(identity, request)
                }
            }
            patch("/entries/{entryId}") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@patch
                if (!call.requireIdempotencyKey()) return@patch
                val version = call.expectedVersion() ?: return@patch
                val entryId = call.parameters["entryId"].orEmpty()
                if (!entryId.isUuid()) return@patch call.respondFinanceError(HttpStatusCode.BadRequest, "Entry id must be a UUID")
                val request = call.receive<FinanceEntryInput>()
                call.respondFinance("/client/business/accounting/entries/$entryId") {
                    service.updateFinanceEntry(identity, entryId, version, request)
                }
            }
            post("/entries/{entryId}/record") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@post
                if (!call.requireIdempotencyKey()) return@post
                val version = call.expectedVersion() ?: return@post
                val entryId = call.parameters["entryId"].orEmpty()
                if (!entryId.isUuid()) return@post call.respondFinanceError(HttpStatusCode.BadRequest, "Entry id must be a UUID")
                call.respondFinance("/client/business/accounting/entries/$entryId/record") {
                    service.recordFinanceEntry(identity, entryId, version)
                }
            }
            post("/entries/{entryId}/void") {
                val identity = call.financeIdentity(resolveIdentity) ?: return@post
                if (!call.requireIdempotencyKey()) return@post
                val version = call.expectedVersion() ?: return@post
                val entryId = call.parameters["entryId"].orEmpty()
                if (!entryId.isUuid()) return@post call.respondFinanceError(HttpStatusCode.BadRequest, "Entry id must be a UUID")
                val request = call.receive<VoidFinanceEntryRequest>()
                call.respondFinance("/client/business/accounting/entries/$entryId/void") {
                    service.voidFinanceEntry(identity, entryId, version, request.reason)
                }
            }
        }
    }
}

@Serializable
data class VoidFinanceEntryRequest(val reason: String = "")

private suspend fun ApplicationCall.financeIdentity(
    resolver: BusinessFinanceIdentityResolver,
): BusinessFinanceClientIdentity? = resolver(this) ?: run {
    respondFinanceError(HttpStatusCode.Forbidden, "Client portal access is required")
    null
}

private suspend fun ApplicationCall.requireIdempotencyKey(): Boolean {
    val value = request.headers["Idempotency-Key"]?.trim().orEmpty()
    if (value.length !in 16..128) {
        respondFinanceError(HttpStatusCode.BadRequest, "A valid Idempotency-Key header is required")
        return false
    }
    return true
}

private suspend fun ApplicationCall.expectedVersion(): Int? {
    val value = request.headers["If-Match"]?.trim()?.removeSurrounding("\"")?.toIntOrNull()
    if (value == null || value <= 0) {
        respondFinanceError(HttpStatusCode.BadRequest, "A positive If-Match version is required")
        return null
    }
    return value
}

private suspend inline fun <reified T> ApplicationCall.respondFinance(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    block: () -> T,
) {
    try {
        respond(status, ApiEnvelope(status = "ok", path = path, data = block()))
    } catch (error: BusinessFinanceForbiddenException) {
        respondFinanceError(HttpStatusCode.Forbidden, error.message ?: "Program authorization is required")
    } catch (error: BusinessFinanceNotFoundException) {
        respondFinanceError(HttpStatusCode.NotFound, error.message ?: "Not found")
    } catch (error: BusinessFinanceConflictException) {
        respondFinanceError(HttpStatusCode.Conflict, error.message ?: "Conflict")
    } catch (error: BusinessFinanceValidationException) {
        respondFinanceError(HttpStatusCode.BadRequest, error.message ?: "Invalid request")
    } catch (error: IllegalArgumentException) {
        respondFinanceError(HttpStatusCode.BadRequest, "Invalid request")
    } catch (error: java.sql.SQLException) {
        respondFinanceError(HttpStatusCode.InternalServerError, "The finance service is temporarily unavailable")
    }
}

private suspend fun ApplicationCall.respondFinanceError(status: HttpStatusCode, message: String) {
    respond(status, ApiErrorEnvelope(status = "error", message = message))
}

private fun String.isUuid(): Boolean = Regex(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
).matches(this)
