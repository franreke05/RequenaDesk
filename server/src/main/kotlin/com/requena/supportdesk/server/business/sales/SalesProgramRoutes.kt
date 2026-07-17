package com.requena.supportdesk.server.business.sales

import com.requena.supportdesk.features.business.sales.data.mapper.BusinessSalesMapper
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesValidationException
import com.requena.supportdesk.features.business.sales.domain.ConvertBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.ExpectedVersionInput
import com.requena.supportdesk.features.business.sales.domain.QuoteTransitionInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessQuoteInput
import com.requena.supportdesk.server.plugins.SensitiveOperationRateLimit
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.errorResponse
import com.requena.supportdesk.server.utils.requireAuthenticatedIdentity
import com.requena.supportdesk.server.utils.respondJson
import com.requena.supportdesk.server.utils.successResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.util.UUID

/**
 * Register from SupportDeskModule once with a store and an entitlement guard.
 * Every endpoint independently derives its tenant from the authenticated JWT.
 */
fun Route.salesProgramRoutes(
    service: SalesProgramService,
    tokenService: SupportDeskTokenService,
) {
    route("/client/apps") {
        route("/customers") {
            get {
                val identity = call.salesIdentity(tokenService) ?: return@get
                val page = call.pageRequest(customerStatuses) ?: return@get
                call.respondSales("/client/apps/customers") { BusinessSalesMapper.pageDto(service.customers(identity, page), BusinessSalesMapper::customerDto) }
            }
            post {
                val identity = call.salesIdentity(tokenService) ?: return@post
                val input = call.receiveSales<CreateBusinessCustomerInput>() ?: return@post
                call.respondSales("/client/apps/customers", HttpStatusCode.Created) { BusinessSalesMapper.customerDto(service.createCustomer(identity, input)) }
            }
            get("/{customerId}") {
                val identity = call.salesIdentity(tokenService) ?: return@get
                val customerId = call.uuidParameter("customerId") ?: return@get
                call.respondSales("/client/apps/customers/$customerId") { BusinessSalesMapper.customerDetailDto(service.customer(identity, customerId)) }
            }
            rateLimit(SensitiveOperationRateLimit) {
                patch("/{customerId}") {
                    val identity = call.salesIdentity(tokenService) ?: return@patch
                    val customerId = call.uuidParameter("customerId") ?: return@patch
                    val input = call.receiveSales<UpdateBusinessCustomerInput>() ?: return@patch
                    call.respondSales("/client/apps/customers/$customerId") { BusinessSalesMapper.customerDto(service.updateCustomer(identity, customerId, input)) }
                }
                post("/{customerId}/archive") {
                    val identity = call.salesIdentity(tokenService) ?: return@post
                    val customerId = call.uuidParameter("customerId") ?: return@post
                    val input = call.receiveSales<ExpectedVersionInput>() ?: return@post
                    call.respondSales("/client/apps/customers/$customerId/archive") { BusinessSalesMapper.customerDto(service.archiveCustomer(identity, customerId, input.expectedVersion)) }
                }
                post("/{customerId}/contacts") {
                    val identity = call.salesIdentity(tokenService) ?: return@post
                    val customerId = call.uuidParameter("customerId") ?: return@post
                    val input = call.receiveSales<CreateBusinessContactInput>() ?: return@post
                    call.respondSales("/client/apps/customers/$customerId/contacts", HttpStatusCode.Created) { BusinessSalesMapper.contactDto(service.createContact(identity, customerId, input)) }
                }
                patch("/{customerId}/contacts/{contactId}") {
                    val identity = call.salesIdentity(tokenService) ?: return@patch
                    val customerId = call.uuidParameter("customerId") ?: return@patch
                    val contactId = call.uuidParameter("contactId") ?: return@patch
                    val input = call.receiveSales<UpdateBusinessContactInput>() ?: return@patch
                    call.respondSales("/client/apps/customers/$customerId/contacts/$contactId") { BusinessSalesMapper.contactDto(service.updateContact(identity, customerId, contactId, input)) }
                }
            }
        }

        route("/catalog") {
            route("/items") {
                get {
                    val identity = call.salesIdentity(tokenService) ?: return@get
                    val page = call.pageRequest(catalogStatuses) ?: return@get
                    call.respondSales("/client/apps/catalog/items") { BusinessSalesMapper.pageDto(service.catalogItems(identity, page), BusinessSalesMapper::catalogItemDto) }
                }
                post {
                    val identity = call.salesIdentity(tokenService) ?: return@post
                    val input = call.receiveSales<CreateBusinessCatalogItemInput>() ?: return@post
                    call.respondSales("/client/apps/catalog/items", HttpStatusCode.Created) { BusinessSalesMapper.catalogItemDto(service.createCatalogItem(identity, input)) }
                }
                rateLimit(SensitiveOperationRateLimit) {
                    patch("/{itemId}") {
                        val identity = call.salesIdentity(tokenService) ?: return@patch
                        val itemId = call.uuidParameter("itemId") ?: return@patch
                        val input = call.receiveSales<UpdateBusinessCatalogItemInput>() ?: return@patch
                        call.respondSales("/client/apps/catalog/items/$itemId") { BusinessSalesMapper.catalogItemDto(service.updateCatalogItem(identity, itemId, input)) }
                    }
                    post("/{itemId}/archive") {
                        val identity = call.salesIdentity(tokenService) ?: return@post
                        val itemId = call.uuidParameter("itemId") ?: return@post
                        val input = call.receiveSales<ExpectedVersionInput>() ?: return@post
                        call.respondSales("/client/apps/catalog/items/$itemId/archive") { BusinessSalesMapper.catalogItemDto(service.archiveCatalogItem(identity, itemId, input.expectedVersion)) }
                    }
                    get("/{itemId}/movements") {
                        val identity = call.salesIdentity(tokenService) ?: return@get
                        val itemId = call.uuidParameter("itemId") ?: return@get
                        val page = call.pageRequest(emptySet()) ?: return@get
                        call.respondSales("/client/apps/catalog/items/$itemId/movements") { BusinessSalesMapper.pageDto(service.stockMovements(identity, itemId, page), BusinessSalesMapper::stockMovementDto) }
                    }
                    post("/{itemId}/adjustments") {
                        val identity = call.salesIdentity(tokenService) ?: return@post
                        val itemId = call.uuidParameter("itemId") ?: return@post
                        val input = call.receiveSales<StockAdjustmentInput>() ?: return@post
                        call.respondSales("/client/apps/catalog/items/$itemId/adjustments", HttpStatusCode.Created) { BusinessSalesMapper.stockMovementDto(service.adjustStock(identity, itemId, input)) }
                    }
                }
            }
            get("/stock") {
                val identity = call.salesIdentity(tokenService) ?: return@get
                val page = call.pageRequest(stockStatuses) ?: return@get
                call.respondSales("/client/apps/catalog/stock") { BusinessSalesMapper.pageDto(service.stock(identity, page), BusinessSalesMapper::stockSummaryDto) }
            }
        }

        route("/sales") {
            route("/quotes") {
                get {
                    val identity = call.salesIdentity(tokenService) ?: return@get
                    val page = call.pageRequest(quoteStatuses) ?: return@get
                    call.respondSales("/client/apps/sales/quotes") { BusinessSalesMapper.pageDto(service.quotes(identity, page), BusinessSalesMapper::quoteDto) }
                }
                post {
                    val identity = call.salesIdentity(tokenService) ?: return@post
                    val input = call.receiveSales<CreateBusinessQuoteInput>() ?: return@post
                    call.respondSales("/client/apps/sales/quotes", HttpStatusCode.Created) { BusinessSalesMapper.quoteDto(service.createQuote(identity, input)) }
                }
                get("/{quoteId}") {
                    val identity = call.salesIdentity(tokenService) ?: return@get
                    val quoteId = call.uuidParameter("quoteId") ?: return@get
                    call.respondSales("/client/apps/sales/quotes/$quoteId") { BusinessSalesMapper.quoteDto(service.quote(identity, quoteId)) }
                }
                rateLimit(SensitiveOperationRateLimit) {
                    patch("/{quoteId}") {
                        val identity = call.salesIdentity(tokenService) ?: return@patch
                        val quoteId = call.uuidParameter("quoteId") ?: return@patch
                        val input = call.receiveSales<UpdateBusinessQuoteInput>() ?: return@patch
                        call.respondSales("/client/apps/sales/quotes/$quoteId") { BusinessSalesMapper.quoteDto(service.updateQuote(identity, quoteId, input)) }
                    }
                    quoteTransitionRoute("mark-sent", BusinessQuoteStatus.SENT, service, tokenService)
                    quoteTransitionRoute("mark-accepted", BusinessQuoteStatus.ACCEPTED, service, tokenService)
                    quoteTransitionRoute("mark-rejected", BusinessQuoteStatus.REJECTED, service, tokenService)
                    quoteTransitionRoute("mark-expired", BusinessQuoteStatus.EXPIRED, service, tokenService)
                    post("/{quoteId}/convert-to-sale") {
                        val identity = call.salesIdentity(tokenService) ?: return@post
                        val quoteId = call.uuidParameter("quoteId") ?: return@post
                        val input = call.receiveSales<ConvertBusinessQuoteInput>() ?: return@post
                        call.respondSales("/client/apps/sales/quotes/$quoteId/convert-to-sale") { BusinessSalesMapper.saleDto(service.convertQuote(identity, quoteId, input)) }
                    }
                }
            }
            route("/sales") {
                get {
                    val identity = call.salesIdentity(tokenService) ?: return@get
                    val page = call.pageRequest(saleStatuses) ?: return@get
                    call.respondSales("/client/apps/sales/sales") { BusinessSalesMapper.pageDto(service.sales(identity, page), BusinessSalesMapper::saleDto) }
                }
                get("/{saleId}") {
                    val identity = call.salesIdentity(tokenService) ?: return@get
                    val saleId = call.uuidParameter("saleId") ?: return@get
                    call.respondSales("/client/apps/sales/sales/$saleId") { BusinessSalesMapper.saleDto(service.sale(identity, saleId)) }
                }
            }
        }
    }
}

private fun Route.quoteTransitionRoute(
    segment: String,
    target: BusinessQuoteStatus,
    service: SalesProgramService,
    tokenService: SupportDeskTokenService,
) {
    post("/{quoteId}/$segment") {
        val identity = call.salesIdentity(tokenService) ?: return@post
        val quoteId = call.uuidParameter("quoteId") ?: return@post
        val input = call.receiveSales<QuoteTransitionInput>() ?: return@post
        call.respondSales("/client/apps/sales/quotes/$quoteId/$segment") { BusinessSalesMapper.quoteDto(service.transitionQuote(identity, quoteId, target, input)) }
    }
}

private suspend fun ApplicationCall.salesIdentity(tokenService: SupportDeskTokenService): SalesProgramIdentity? {
    val identity = requireAuthenticatedIdentity(tokenService) ?: return null
    if (identity.role == "CLIENT" && !identity.clientId.isNullOrBlank()) return SalesProgramIdentity(identity.userId, requireNotNull(identity.clientId))
    respondJson(HttpStatusCode.Forbidden, errorResponse("Client portal access is required"))
    return null
}

private suspend fun ApplicationCall.pageRequest(allowedStatuses: Set<String>): BusinessSalesPageRequest? {
    val query = request.queryParameters["query"]?.trim()?.takeIf(String::isNotBlank)
    val status = request.queryParameters["status"]?.trim()?.uppercase()?.takeIf(String::isNotBlank)
    val cursor = request.queryParameters["cursor"]?.trim()?.takeIf(String::isNotBlank)
    val limit = request.queryParameters["limit"]?.toIntOrNull() ?: 40
    if (query != null && query.length > 160 || cursor != null && !cursor.isUuid() || limit !in 1..100 || status != null && status !in allowedStatuses) {
        respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid list filters"))
        return null
    }
    return BusinessSalesPageRequest(query = query, status = status, cursor = cursor, limit = limit)
}

private suspend fun ApplicationCall.uuidParameter(name: String): String? {
    val value = parameters[name].orEmpty()
    if (value.isUuid()) return value
    respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid identifier"))
    return null
}

private suspend inline fun <reified T : Any> ApplicationCall.receiveSales(): T? = try {
    receive<T>()
} catch (_: Throwable) {
    respondJson(HttpStatusCode.BadRequest, errorResponse("Invalid request payload"))
    null
}

private suspend inline fun <reified T> ApplicationCall.respondSales(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    block: () -> T,
) {
    try {
        respondJson(status, successResponse(path, Json.encodeToJsonElement(block())))
    } catch (_: SalesProgramForbiddenException) {
        respondJson(HttpStatusCode.Forbidden, errorResponse("This beta program requires administrator authorization"))
    } catch (_: SalesProgramNotFoundException) {
        respondJson(HttpStatusCode.NotFound, errorResponse("Business record was not found"))
    } catch (error: SalesProgramConflictException) {
        respondJson(HttpStatusCode.Conflict, errorResponse(error.message ?: "Business conflict"))
    } catch (error: BusinessSalesValidationException) {
        respondJson(HttpStatusCode.UnprocessableEntity, errorResponse(error.message ?: "Invalid business data"))
    } catch (error: IllegalArgumentException) {
        respondJson(HttpStatusCode.UnprocessableEntity, errorResponse(error.message ?: "Invalid business data"))
    }
}

private fun String?.isUuid(): Boolean = this != null && runCatching { UUID.fromString(this) }.isSuccess

private val customerStatuses = setOf("ACTIVE", "ARCHIVED")
private val catalogStatuses = setOf("ACTIVE", "ARCHIVED", "PRODUCT", "SERVICE")
private val stockStatuses = setOf("LOW")
private val quoteStatuses = BusinessQuoteStatus.entries.mapTo(mutableSetOf()) { it.name }
private val saleStatuses = setOf("CONFIRMED", "CANCELLED")
