package com.requena.supportdesk.features.business.sales.domain

import com.requena.supportdesk.core.result.AppResult

/** Client contract; the server remains authoritative for access, totals, stock and transitions. */
interface BusinessSalesRepository {
    suspend fun customers(request: BusinessSalesPageRequest = BusinessSalesPageRequest()): AppResult<BusinessSalesPage<BusinessCustomer>>
    suspend fun customer(customerId: String): AppResult<BusinessCustomerDetail>
    suspend fun createCustomer(input: CreateBusinessCustomerInput): AppResult<BusinessCustomer>
    suspend fun updateCustomer(customerId: String, input: UpdateBusinessCustomerInput): AppResult<BusinessCustomer>
    suspend fun archiveCustomer(customerId: String, expectedVersion: Int): AppResult<BusinessCustomer>
    suspend fun createContact(customerId: String, input: CreateBusinessContactInput): AppResult<BusinessContact>
    suspend fun updateContact(customerId: String, contactId: String, input: UpdateBusinessContactInput): AppResult<BusinessContact>

    suspend fun catalogItems(request: BusinessSalesPageRequest = BusinessSalesPageRequest()): AppResult<BusinessSalesPage<BusinessCatalogItem>>
    suspend fun createCatalogItem(input: CreateBusinessCatalogItemInput): AppResult<BusinessCatalogItem>
    suspend fun updateCatalogItem(itemId: String, input: UpdateBusinessCatalogItemInput): AppResult<BusinessCatalogItem>
    suspend fun archiveCatalogItem(itemId: String, expectedVersion: Int): AppResult<BusinessCatalogItem>
    suspend fun stock(request: BusinessSalesPageRequest = BusinessSalesPageRequest()): AppResult<BusinessSalesPage<BusinessStockSummary>>
    suspend fun stockMovements(itemId: String, request: BusinessSalesPageRequest = BusinessSalesPageRequest()): AppResult<BusinessSalesPage<BusinessStockMovement>>
    suspend fun adjustStock(itemId: String, input: StockAdjustmentInput): AppResult<BusinessStockMovement>

    suspend fun quotes(request: BusinessSalesPageRequest = BusinessSalesPageRequest()): AppResult<BusinessSalesPage<BusinessQuote>>
    suspend fun quote(quoteId: String): AppResult<BusinessQuote>
    suspend fun createQuote(input: CreateBusinessQuoteInput): AppResult<BusinessQuote>
    suspend fun updateQuote(quoteId: String, input: UpdateBusinessQuoteInput): AppResult<BusinessQuote>
    suspend fun transitionQuote(quoteId: String, target: BusinessQuoteStatus, input: QuoteTransitionInput): AppResult<BusinessQuote>
    suspend fun convertQuote(quoteId: String, input: ConvertBusinessQuoteInput): AppResult<BusinessSale>
    suspend fun sales(request: BusinessSalesPageRequest = BusinessSalesPageRequest()): AppResult<BusinessSalesPage<BusinessSale>>
    suspend fun sale(saleId: String): AppResult<BusinessSale>
}
