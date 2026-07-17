package com.requena.supportdesk.features.business.sales.data.datasource

import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.business.sales.data.dto.BusinessCatalogItemDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessContactDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessCustomerDetailDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessCustomerDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessQuoteDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessSaleDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessSalesPageDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessStockMovementDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessStockSummaryDto
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPageRequest
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
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLParameter

interface BusinessSalesDataSource {
    suspend fun customers(request: BusinessSalesPageRequest): BusinessSalesPageDto<BusinessCustomerDto>
    suspend fun customer(customerId: String): BusinessCustomerDetailDto
    suspend fun createCustomer(input: CreateBusinessCustomerInput): BusinessCustomerDto
    suspend fun updateCustomer(customerId: String, input: UpdateBusinessCustomerInput): BusinessCustomerDto
    suspend fun archiveCustomer(customerId: String, expectedVersion: Int): BusinessCustomerDto
    suspend fun createContact(customerId: String, input: CreateBusinessContactInput): BusinessContactDto
    suspend fun updateContact(customerId: String, contactId: String, input: UpdateBusinessContactInput): BusinessContactDto

    suspend fun catalogItems(request: BusinessSalesPageRequest): BusinessSalesPageDto<BusinessCatalogItemDto>
    suspend fun createCatalogItem(input: CreateBusinessCatalogItemInput): BusinessCatalogItemDto
    suspend fun updateCatalogItem(itemId: String, input: UpdateBusinessCatalogItemInput): BusinessCatalogItemDto
    suspend fun archiveCatalogItem(itemId: String, expectedVersion: Int): BusinessCatalogItemDto
    suspend fun stock(request: BusinessSalesPageRequest): BusinessSalesPageDto<BusinessStockSummaryDto>
    suspend fun stockMovements(itemId: String, request: BusinessSalesPageRequest): BusinessSalesPageDto<BusinessStockMovementDto>
    suspend fun adjustStock(itemId: String, input: StockAdjustmentInput): BusinessStockMovementDto

    suspend fun quotes(request: BusinessSalesPageRequest): BusinessSalesPageDto<BusinessQuoteDto>
    suspend fun quote(quoteId: String): BusinessQuoteDto
    suspend fun createQuote(input: CreateBusinessQuoteInput): BusinessQuoteDto
    suspend fun updateQuote(quoteId: String, input: UpdateBusinessQuoteInput): BusinessQuoteDto
    suspend fun transitionQuote(quoteId: String, target: BusinessQuoteStatus, input: QuoteTransitionInput): BusinessQuoteDto
    suspend fun convertQuote(quoteId: String, input: ConvertBusinessQuoteInput): BusinessSaleDto
    suspend fun sales(request: BusinessSalesPageRequest): BusinessSalesPageDto<BusinessSaleDto>
    suspend fun sale(saleId: String): BusinessSaleDto
}

class RemoteBusinessSalesDataSource(
    private val httpClient: HttpClient,
) : BusinessSalesDataSource {
    override suspend fun customers(request: BusinessSalesPageRequest) =
        httpClient.get(pageUrl("/client/apps/customers", request)).requireApiData<BusinessSalesPageDto<BusinessCustomerDto>>()

    override suspend fun customer(customerId: String) =
        httpClient.get("${supportDeskBaseUrl()}/client/apps/customers/$customerId").requireApiData<BusinessCustomerDetailDto>()

    override suspend fun createCustomer(input: CreateBusinessCustomerInput) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/customers") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessCustomerDto>()

    override suspend fun updateCustomer(customerId: String, input: UpdateBusinessCustomerInput) =
        httpClient.patch("${supportDeskBaseUrl()}/client/apps/customers/$customerId") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessCustomerDto>()

    override suspend fun archiveCustomer(customerId: String, expectedVersion: Int) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/customers/$customerId/archive") {
            setBody(jsonRequestBody(ExpectedVersionInput(expectedVersion)))
        }.requireApiData<BusinessCustomerDto>()

    override suspend fun createContact(customerId: String, input: CreateBusinessContactInput) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/customers/$customerId/contacts") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessContactDto>()

    override suspend fun updateContact(customerId: String, contactId: String, input: UpdateBusinessContactInput) =
        httpClient.patch("${supportDeskBaseUrl()}/client/apps/customers/$customerId/contacts/$contactId") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessContactDto>()

    override suspend fun catalogItems(request: BusinessSalesPageRequest) =
        httpClient.get(pageUrl("/client/apps/catalog/items", request)).requireApiData<BusinessSalesPageDto<BusinessCatalogItemDto>>()

    override suspend fun createCatalogItem(input: CreateBusinessCatalogItemInput) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/catalog/items") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessCatalogItemDto>()

    override suspend fun updateCatalogItem(itemId: String, input: UpdateBusinessCatalogItemInput) =
        httpClient.patch("${supportDeskBaseUrl()}/client/apps/catalog/items/$itemId") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessCatalogItemDto>()

    override suspend fun archiveCatalogItem(itemId: String, expectedVersion: Int) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/catalog/items/$itemId/archive") {
            setBody(jsonRequestBody(ExpectedVersionInput(expectedVersion)))
        }.requireApiData<BusinessCatalogItemDto>()

    override suspend fun stock(request: BusinessSalesPageRequest) =
        httpClient.get(pageUrl("/client/apps/catalog/stock", request)).requireApiData<BusinessSalesPageDto<BusinessStockSummaryDto>>()

    override suspend fun stockMovements(itemId: String, request: BusinessSalesPageRequest) =
        httpClient.get(pageUrl("/client/apps/catalog/items/$itemId/movements", request)).requireApiData<BusinessSalesPageDto<BusinessStockMovementDto>>()

    override suspend fun adjustStock(itemId: String, input: StockAdjustmentInput) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/catalog/items/$itemId/adjustments") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessStockMovementDto>()

    override suspend fun quotes(request: BusinessSalesPageRequest) =
        httpClient.get(pageUrl("/client/apps/sales/quotes", request)).requireApiData<BusinessSalesPageDto<BusinessQuoteDto>>()

    override suspend fun quote(quoteId: String) =
        httpClient.get("${supportDeskBaseUrl()}/client/apps/sales/quotes/$quoteId").requireApiData<BusinessQuoteDto>()

    override suspend fun createQuote(input: CreateBusinessQuoteInput) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/sales/quotes") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessQuoteDto>()

    override suspend fun updateQuote(quoteId: String, input: UpdateBusinessQuoteInput) =
        httpClient.patch("${supportDeskBaseUrl()}/client/apps/sales/quotes/$quoteId") { setBody(jsonRequestBody(input)) }.requireApiData<BusinessQuoteDto>()

    override suspend fun transitionQuote(quoteId: String, target: BusinessQuoteStatus, input: QuoteTransitionInput) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/sales/quotes/$quoteId/${target.routeSegment()}") {
            setBody(jsonRequestBody(input))
        }.requireApiData<BusinessQuoteDto>()

    override suspend fun convertQuote(quoteId: String, input: ConvertBusinessQuoteInput) =
        httpClient.post("${supportDeskBaseUrl()}/client/apps/sales/quotes/$quoteId/convert-to-sale") {
            setBody(jsonRequestBody(input))
        }.requireApiData<BusinessSaleDto>()

    override suspend fun sales(request: BusinessSalesPageRequest) =
        httpClient.get(pageUrl("/client/apps/sales/sales", request)).requireApiData<BusinessSalesPageDto<BusinessSaleDto>>()

    override suspend fun sale(saleId: String) =
        httpClient.get("${supportDeskBaseUrl()}/client/apps/sales/sales/$saleId").requireApiData<BusinessSaleDto>()

    private fun pageUrl(path: String, request: BusinessSalesPageRequest): String {
        val parameters = listOfNotNull(
            request.query?.takeIf(String::isNotBlank)?.let { "query=${it.encodeURLParameter()}" },
            request.status?.takeIf(String::isNotBlank)?.let { "status=${it.encodeURLParameter()}" },
            request.cursor?.takeIf(String::isNotBlank)?.let { "cursor=${it.encodeURLParameter()}" },
            "limit=${request.limit}",
        )
        return "${supportDeskBaseUrl()}$path?${parameters.joinToString("&")}" 
    }
}

private fun BusinessQuoteStatus.routeSegment(): String = when (this) {
    BusinessQuoteStatus.SENT -> "mark-sent"
    BusinessQuoteStatus.ACCEPTED -> "mark-accepted"
    BusinessQuoteStatus.REJECTED -> "mark-rejected"
    BusinessQuoteStatus.EXPIRED -> "mark-expired"
    BusinessQuoteStatus.DRAFT -> error("Draft is not a transition target")
}
