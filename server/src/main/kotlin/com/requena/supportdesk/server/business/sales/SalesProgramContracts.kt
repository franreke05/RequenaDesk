package com.requena.supportdesk.server.business.sales

import com.requena.supportdesk.features.business.sales.domain.BUSINESS_CATALOG
import com.requena.supportdesk.features.business.sales.domain.BUSINESS_CUSTOMERS
import com.requena.supportdesk.features.business.sales.domain.BUSINESS_QUOTES
import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItem
import com.requena.supportdesk.features.business.sales.domain.BusinessContact
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessQuote
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSale
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPage
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovement
import com.requena.supportdesk.features.business.sales.domain.BusinessStockSummary
import com.requena.supportdesk.features.business.sales.domain.CalculatedBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.CalculatedBusinessQuoteUpdate
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.CreateBusinessCustomerInput
import com.requena.supportdesk.features.business.sales.domain.ConvertBusinessQuoteInput
import com.requena.supportdesk.features.business.sales.domain.StockAdjustmentInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCatalogItemInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessContactInput
import com.requena.supportdesk.features.business.sales.domain.UpdateBusinessCustomerInput

data class SalesProgramIdentity(
    val userId: String,
    val clientId: String,
)

/**
 * Integration boundary for V8/V9 subscriptions. The application module must inject an
 * implementation that derives access from an ACTIVE subscription; UI visibility is never authority.
 */
fun interface SalesProgramAccessGuard {
    fun hasActiveEntitlement(identity: SalesProgramIdentity, productKey: String): Boolean
}

interface SalesProgramStore {
    fun customers(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessCustomer>
    fun customer(clientId: String, customerId: String): BusinessCustomerDetail?
    fun createCustomer(clientId: String, actorUserId: String, input: CreateBusinessCustomerInput): BusinessCustomer
    fun updateCustomer(clientId: String, actorUserId: String, customerId: String, input: UpdateBusinessCustomerInput): BusinessCustomer
    fun archiveCustomer(clientId: String, actorUserId: String, customerId: String, expectedVersion: Int): BusinessCustomer
    fun createContact(clientId: String, actorUserId: String, customerId: String, input: CreateBusinessContactInput): BusinessContact
    fun updateContact(clientId: String, actorUserId: String, customerId: String, contactId: String, input: UpdateBusinessContactInput): BusinessContact

    fun catalogItems(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessCatalogItem>
    fun createCatalogItem(clientId: String, actorUserId: String, input: CreateBusinessCatalogItemInput): BusinessCatalogItem
    fun updateCatalogItem(clientId: String, actorUserId: String, itemId: String, input: UpdateBusinessCatalogItemInput): BusinessCatalogItem
    fun archiveCatalogItem(clientId: String, actorUserId: String, itemId: String, expectedVersion: Int): BusinessCatalogItem
    fun stock(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessStockSummary>
    fun stockMovements(clientId: String, itemId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessStockMovement>
    fun adjustStock(clientId: String, actorUserId: String, itemId: String, input: StockAdjustmentInput): BusinessStockMovement

    fun quotes(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessQuote>
    fun quote(clientId: String, quoteId: String): BusinessQuote?
    fun createQuote(clientId: String, actorUserId: String, input: CalculatedBusinessQuoteInput): BusinessQuote
    fun updateQuote(clientId: String, actorUserId: String, quoteId: String, input: CalculatedBusinessQuoteUpdate): BusinessQuote
    fun transitionQuote(clientId: String, actorUserId: String, quoteId: String, target: BusinessQuoteStatus, expectedVersion: Int): BusinessQuote
    fun convertQuote(clientId: String, actorUserId: String, quoteId: String, input: ConvertBusinessQuoteInput): BusinessSale
    fun sales(clientId: String, request: BusinessSalesPageRequest): BusinessSalesPage<BusinessSale>
    fun sale(clientId: String, saleId: String): BusinessSale?
}

class SalesProgramForbiddenException : RuntimeException("This beta program requires administrator authorization")
class SalesProgramNotFoundException : RuntimeException("Business record was not found")
class SalesProgramConflictException(message: String) : RuntimeException(message)

internal fun requireSalesEntitlement(
    accessGuard: SalesProgramAccessGuard,
    identity: SalesProgramIdentity,
    productKey: String,
) {
    if (!accessGuard.hasActiveEntitlement(identity, productKey)) throw SalesProgramForbiddenException()
}

internal val SalesProgramKeys = setOf(BUSINESS_CUSTOMERS, BUSINESS_QUOTES, BUSINESS_CATALOG)
