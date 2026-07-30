package com.requena.supportdesk.server.business.sales

import com.requena.supportdesk.features.business.sales.domain.BUSINESS_CATALOG
import com.requena.supportdesk.features.business.sales.domain.BUSINESS_CUSTOMERS
import com.requena.supportdesk.features.business.sales.domain.BUSINESS_QUOTES
import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItem
import com.requena.supportdesk.features.business.sales.domain.BusinessContact
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerTag
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerActivity
import com.requena.supportdesk.features.business.sales.domain.BusinessQuote
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSale
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPage
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesRules
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesValidationException
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovement
import com.requena.supportdesk.features.business.sales.domain.BusinessStockSummary
import com.requena.supportdesk.features.business.sales.domain.ConvertBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerTagInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerActivityInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.QuoteTransitionInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerTagInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerActivityInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessQuoteInput

/**
 * Server-authoritative beta vertical. It is intentionally independent from the legacy CRM,
 * invoices and SupportDeskService. Registration supplies the subscription guard and route wiring.
 */
class SalesProgramService(
    private val store: SalesProgramStore,
    private val accessGuard: SalesProgramAccessGuard,
) {
    fun customers(identity: SalesProgramIdentity, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessCustomer> {
        requireCustomers(identity)
        return store.customers(identity.clientId, request.validated())
    }

    fun customer(identity: SalesProgramIdentity, customerId: String): BusinessCustomerDetail {
        requireCustomers(identity)
        return store.customer(identity.clientId, customerId) ?: throw SalesProgramNotFoundException()
    }

    fun createCustomer(identity: SalesProgramIdentity, input: CreateBusinessCustomerInput): BusinessCustomer {
        requireCustomers(identity)
        BusinessSalesRules.validateCustomer(input)
        return store.createCustomer(identity.clientId, identity.userId, input.normalized())
    }

    fun updateCustomer(identity: SalesProgramIdentity, customerId: String, input: UpdateBusinessCustomerInput): BusinessCustomer {
        requireCustomers(identity)
        BusinessSalesRules.validateCustomer(input)
        return store.updateCustomer(identity.clientId, identity.userId, customerId, input.normalized())
    }

    fun archiveCustomer(identity: SalesProgramIdentity, customerId: String, expectedVersion: Int): BusinessCustomer {
        requireCustomers(identity)
        require(expectedVersion > 0) { "Expected version is invalid" }
        return store.archiveCustomer(identity.clientId, identity.userId, customerId, expectedVersion)
    }

    fun restoreCustomer(identity: SalesProgramIdentity, customerId: String, expectedVersion: Int): BusinessCustomer {
        requireCustomers(identity)
        require(expectedVersion > 0) { "Expected version is invalid" }
        return store.restoreCustomer(identity.clientId, identity.userId, customerId, expectedVersion)
    }

    fun createContact(identity: SalesProgramIdentity, customerId: String, input: CreateBusinessContactInput): BusinessContact {
        requireCustomers(identity)
        BusinessSalesRules.validateContact(input)
        return store.createContact(identity.clientId, identity.userId, customerId, input.normalized())
    }

    fun updateContact(identity: SalesProgramIdentity, customerId: String, contactId: String, input: UpdateBusinessContactInput): BusinessContact {
        requireCustomers(identity)
        BusinessSalesRules.validateContact(input)
        return store.updateContact(identity.clientId, identity.userId, customerId, contactId, input.normalized())
    }

    fun customerTags(identity: SalesProgramIdentity): List<BusinessCustomerTag> {
        requireCustomers(identity)
        return store.customerTags(identity.clientId)
    }

    fun createCustomerTag(identity: SalesProgramIdentity, input: CreateBusinessCustomerTagInput): BusinessCustomerTag {
        requireCustomers(identity)
        BusinessSalesRules.validateCustomerTag(input)
        return store.createCustomerTag(identity.clientId, identity.userId, input.copy(name = input.name.trim(), colorHex = input.colorHex.uppercase()))
    }

    fun updateCustomerTag(identity: SalesProgramIdentity, tagId: String, input: UpdateBusinessCustomerTagInput): BusinessCustomerTag {
        requireCustomers(identity)
        BusinessSalesRules.validateCustomerTag(input)
        return store.updateCustomerTag(identity.clientId, identity.userId, tagId, input.copy(name = input.name.trim(), colorHex = input.colorHex.uppercase()))
    }

    fun archiveCustomerTag(identity: SalesProgramIdentity, tagId: String, expectedVersion: Int): BusinessCustomerTag {
        requireCustomers(identity)
        require(expectedVersion > 0) { "Expected version is invalid" }
        return store.archiveCustomerTag(identity.clientId, identity.userId, tagId, expectedVersion)
    }

    fun customerActivities(identity: SalesProgramIdentity, customerId: String): List<BusinessCustomerActivity> {
        requireCustomers(identity)
        return store.customerActivities(identity.clientId, customerId)
    }

    fun createCustomerActivity(
        identity: SalesProgramIdentity,
        customerId: String,
        input: CreateBusinessCustomerActivityInput,
    ): BusinessCustomerActivity {
        requireCustomers(identity)
        BusinessSalesRules.validateCustomerActivity(input)
        validateActivityTimestamps(input.occurredAt, input.dueAt)
        val normalized = input.normalized()
        val timestamped = if (normalized.occurredAt == null && normalized.dueAt == null) {
            normalized.copy(occurredAt = java.time.Instant.now().toString())
        } else {
            normalized
        }
        return store.createCustomerActivity(identity.clientId, identity.userId, customerId, timestamped)
    }

    fun updateCustomerActivity(
        identity: SalesProgramIdentity,
        customerId: String,
        activityId: String,
        input: UpdateBusinessCustomerActivityInput,
    ): BusinessCustomerActivity {
        requireCustomers(identity)
        BusinessSalesRules.validateCustomerActivity(input)
        validateActivityTimestamps(input.occurredAt, input.dueAt)
        return store.updateCustomerActivity(identity.clientId, identity.userId, customerId, activityId, input.normalized())
    }

    fun catalogItems(identity: SalesProgramIdentity, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessCatalogItem> {
        requireCatalog(identity)
        return store.catalogItems(identity.clientId, request.validated())
    }

    fun createCatalogItem(identity: SalesProgramIdentity, input: CreateBusinessCatalogItemInput): BusinessCatalogItem {
        requireCatalog(identity)
        BusinessSalesRules.validateCatalogItem(input)
        return store.createCatalogItem(identity.clientId, identity.userId, input.normalized())
    }

    fun updateCatalogItem(identity: SalesProgramIdentity, itemId: String, input: UpdateBusinessCatalogItemInput): BusinessCatalogItem {
        requireCatalog(identity)
        return store.updateCatalogItem(identity.clientId, identity.userId, itemId, input.normalized())
    }

    fun archiveCatalogItem(identity: SalesProgramIdentity, itemId: String, expectedVersion: Int): BusinessCatalogItem {
        requireCatalog(identity)
        require(expectedVersion > 0) { "Expected version is invalid" }
        return store.archiveCatalogItem(identity.clientId, identity.userId, itemId, expectedVersion)
    }

    fun stock(identity: SalesProgramIdentity, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessStockSummary> {
        requireCatalog(identity)
        return store.stock(identity.clientId, request.validated())
    }

    fun stockMovements(identity: SalesProgramIdentity, itemId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessStockMovement> {
        requireCatalog(identity)
        return store.stockMovements(identity.clientId, itemId, request.validated())
    }

    fun adjustStock(identity: SalesProgramIdentity, itemId: String, input: StockAdjustmentInput): BusinessStockMovement {
        requireCatalog(identity)
        BusinessSalesRules.validateStockAdjustment(input)
        return store.adjustStock(identity.clientId, identity.userId, itemId, input.copy(reason = input.reason.trim(), idempotencyKey = input.idempotencyKey.trim()))
    }

    fun quotes(identity: SalesProgramIdentity, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessQuote> {
        requireQuotes(identity)
        return store.quotes(identity.clientId, request.validated())
    }

    fun quote(identity: SalesProgramIdentity, quoteId: String): BusinessQuote {
        requireQuotes(identity)
        return store.quote(identity.clientId, quoteId) ?: throw SalesProgramNotFoundException()
    }

    fun createQuote(identity: SalesProgramIdentity, input: CreateBusinessQuoteInput): BusinessQuote {
        requireQuotes(identity)
        return store.createQuote(identity.clientId, identity.userId, BusinessSalesRules.calculateQuote(input))
    }

    fun updateQuote(identity: SalesProgramIdentity, quoteId: String, input: UpdateBusinessQuoteInput): BusinessQuote {
        requireQuotes(identity)
        return store.updateQuote(identity.clientId, identity.userId, quoteId, BusinessSalesRules.calculateQuote(input))
    }

    fun transitionQuote(
        identity: SalesProgramIdentity,
        quoteId: String,
        target: BusinessQuoteStatus,
        input: QuoteTransitionInput,
    ): BusinessQuote {
        requireQuotes(identity)
        BusinessSalesRules.validateTransition(input)
        if (target == BusinessQuoteStatus.DRAFT) throw IllegalArgumentException("Draft is not a transition target")
        return store.transitionQuote(identity.clientId, identity.userId, quoteId, target, input.expectedVersion)
    }

    fun convertQuote(identity: SalesProgramIdentity, quoteId: String, input: ConvertBusinessQuoteInput): BusinessSale {
        requireQuotes(identity)
        BusinessSalesRules.validateIdempotencyKey(input.idempotencyKey)
        return store.convertQuote(identity.clientId, identity.userId, quoteId, input.copy(idempotencyKey = input.idempotencyKey.trim()))
    }

    fun sales(identity: SalesProgramIdentity, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessSale> {
        requireQuotes(identity)
        return store.sales(identity.clientId, request.validated())
    }

    fun sale(identity: SalesProgramIdentity, saleId: String): BusinessSale {
        requireQuotes(identity)
        return store.sale(identity.clientId, saleId) ?: throw SalesProgramNotFoundException()
    }

    private fun requireCustomers(identity: SalesProgramIdentity) = requireSalesEntitlement(accessGuard, identity, BUSINESS_CUSTOMERS)
    private fun requireCatalog(identity: SalesProgramIdentity) = requireSalesEntitlement(accessGuard, identity, BUSINESS_CATALOG)
    private fun requireQuotes(identity: SalesProgramIdentity) = requireSalesEntitlement(accessGuard, identity, BUSINESS_QUOTES)
}

private fun validateActivityTimestamps(occurredAt: String?, dueAt: String?) {
    listOfNotNull(occurredAt, dueAt).forEach { value ->
        if (runCatching { java.time.Instant.parse(value) }.isFailure) {
            throw BusinessSalesValidationException("Activity timestamps must use ISO-8601 UTC format")
        }
    }
}

private fun BusinessSalesPageRequest.validated(): BusinessSalesPageRequest {
    val requestLimit = limit
    val requestQuery = query
    val requestCursor = cursor
    val requestStatus = status
    val requestTagId = tagId
    require(requestLimit in 1..100) { "Limit must be between 1 and 100" }
    require(requestQuery == null || requestQuery.length <= 160) { "Search query is invalid" }
    require(requestCursor == null || requestCursor.length <= 64) { "Cursor is invalid" }
    require(requestStatus == null || requestStatus.length <= 20) { "Status filter is invalid" }
    require(requestTagId == null || requestTagId.length <= 64) { "Tag filter is invalid" }
    return copy(
        query = requestQuery?.trim()?.takeIf(String::isNotBlank),
        status = requestStatus?.trim()?.uppercase()?.takeIf(String::isNotBlank),
        tagId = requestTagId?.trim()?.takeIf(String::isNotBlank),
    )
}

private fun CreateBusinessCustomerInput.normalized() = copy(
    displayName = displayName.trim(), taxId = taxId?.trim()?.takeIf(String::isNotBlank), email = email?.trim()?.takeIf(String::isNotBlank),
    phone = phone?.trim()?.takeIf(String::isNotBlank), address = address?.trim()?.takeIf(String::isNotBlank),
    source = source?.trim()?.takeIf(String::isNotBlank), tagIds = tagIds.distinct(),
)

private fun UpdateBusinessCustomerInput.normalized() = copy(
    displayName = displayName.trim(), taxId = taxId?.trim()?.takeIf(String::isNotBlank), email = email?.trim()?.takeIf(String::isNotBlank),
    phone = phone?.trim()?.takeIf(String::isNotBlank), address = address?.trim()?.takeIf(String::isNotBlank),
    source = source?.trim()?.takeIf(String::isNotBlank), tagIds = tagIds.distinct(),
)

private fun CreateBusinessCustomerActivityInput.normalized() = copy(
    subject = subject.trim(),
    details = details?.trim()?.takeIf(String::isNotBlank),
)

private fun UpdateBusinessCustomerActivityInput.normalized() = copy(
    subject = subject.trim(),
    details = details?.trim()?.takeIf(String::isNotBlank),
)

private fun CreateBusinessContactInput.normalized() = copy(
    fullName = fullName.trim(), role = role?.trim()?.takeIf(String::isNotBlank), email = email?.trim()?.takeIf(String::isNotBlank), phone = phone?.trim()?.takeIf(String::isNotBlank),
)

private fun UpdateBusinessContactInput.normalized() = copy(
    fullName = fullName.trim(), role = role?.trim()?.takeIf(String::isNotBlank), email = email?.trim()?.takeIf(String::isNotBlank), phone = phone?.trim()?.takeIf(String::isNotBlank),
)

private fun CreateBusinessCatalogItemInput.normalized() = copy(
    name = name.trim(), sku = sku?.trim()?.uppercase()?.takeIf(String::isNotBlank), description = description?.trim()?.takeIf(String::isNotBlank), unit = unit.trim(),
)

private fun UpdateBusinessCatalogItemInput.normalized() = copy(
    name = name.trim(), sku = sku?.trim()?.uppercase()?.takeIf(String::isNotBlank), description = description?.trim()?.takeIf(String::isNotBlank), unit = unit.trim(),
)
