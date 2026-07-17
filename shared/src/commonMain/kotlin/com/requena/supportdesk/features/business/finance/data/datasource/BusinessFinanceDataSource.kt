package com.requena.supportdesk.features.business.finance.data.datasource

import com.requena.supportdesk.core.network.ApiEnvelope
import com.requena.supportdesk.core.network.ApiErrorEnvelope
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.business.finance.data.dto.BusinessFinanceEntryDto
import com.requena.supportdesk.features.business.finance.data.dto.BusinessSalesDocumentDto
import com.requena.supportdesk.features.business.finance.data.dto.FinanceEntryRequestDto
import com.requena.supportdesk.features.business.finance.data.dto.FinanceOverviewDto
import com.requena.supportdesk.features.business.finance.data.dto.SalesDocumentDraftRequestDto
import com.requena.supportdesk.features.business.finance.data.dto.VoidFinanceEntryRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json

interface BusinessFinanceDataSource {
    suspend fun listSalesDocuments(): List<BusinessSalesDocumentDto>
    suspend fun createSalesDraft(
        input: SalesDocumentDraftRequestDto,
        idempotencyKey: String,
    ): BusinessSalesDocumentDto
    suspend fun updateSalesDraft(
        id: String,
        expectedVersion: Int,
        input: SalesDocumentDraftRequestDto,
        idempotencyKey: String,
    ): BusinessSalesDocumentDto
    suspend fun archiveSalesDocument(
        id: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): BusinessSalesDocumentDto
    suspend fun listFinanceEntries(): List<BusinessFinanceEntryDto>
    suspend fun createFinanceEntry(
        input: FinanceEntryRequestDto,
        idempotencyKey: String,
    ): BusinessFinanceEntryDto
    suspend fun updateFinanceEntry(
        id: String,
        expectedVersion: Int,
        input: FinanceEntryRequestDto,
        idempotencyKey: String,
    ): BusinessFinanceEntryDto
    suspend fun recordFinanceEntry(
        id: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): BusinessFinanceEntryDto
    suspend fun voidFinanceEntry(
        id: String,
        expectedVersion: Int,
        reason: String,
        idempotencyKey: String,
    ): BusinessFinanceEntryDto
    suspend fun financeOverview(period: String): FinanceOverviewDto
}

/**
 * HTTP boundary for the authenticated client portal. Supply the shared
 * configuredSupportDeskHttpClient(sessionManager), never an anonymous client,
 * so all requests carry the current bearer session and refresh it on 401.
 */
class RemoteBusinessFinanceDataSource(
    private val httpClient: HttpClient,
) : BusinessFinanceDataSource {
    override suspend fun listSalesDocuments(): List<BusinessSalesDocumentDto> =
        httpClient.get(url(INVOICING_DOCUMENTS_PATH)).requireBusinessFinanceData()

    override suspend fun createSalesDraft(
        input: SalesDocumentDraftRequestDto,
        idempotencyKey: String,
    ): BusinessSalesDocumentDto = httpClient.post(url(INVOICING_DOCUMENTS_PATH)) {
        addIdempotencyKey(idempotencyKey)
        setBody(jsonRequestBody(input))
    }.requireBusinessFinanceData()

    override suspend fun updateSalesDraft(
        id: String,
        expectedVersion: Int,
        input: SalesDocumentDraftRequestDto,
        idempotencyKey: String,
    ): BusinessSalesDocumentDto = httpClient.patch(url("$INVOICING_DOCUMENTS_PATH/$id")) {
        addVersionHeaders(idempotencyKey, expectedVersion)
        setBody(jsonRequestBody(input))
    }.requireBusinessFinanceData()

    override suspend fun archiveSalesDocument(
        id: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): BusinessSalesDocumentDto = httpClient.post(url("$INVOICING_DOCUMENTS_PATH/$id/archive")) {
        addVersionHeaders(idempotencyKey, expectedVersion)
    }.requireBusinessFinanceData()

    override suspend fun listFinanceEntries(): List<BusinessFinanceEntryDto> =
        httpClient.get(url(ACCOUNTING_ENTRIES_PATH)).requireBusinessFinanceData()

    override suspend fun createFinanceEntry(
        input: FinanceEntryRequestDto,
        idempotencyKey: String,
    ): BusinessFinanceEntryDto = httpClient.post(url(ACCOUNTING_ENTRIES_PATH)) {
        addIdempotencyKey(idempotencyKey)
        setBody(jsonRequestBody(input))
    }.requireBusinessFinanceData()

    override suspend fun updateFinanceEntry(
        id: String,
        expectedVersion: Int,
        input: FinanceEntryRequestDto,
        idempotencyKey: String,
    ): BusinessFinanceEntryDto = httpClient.patch(url("$ACCOUNTING_ENTRIES_PATH/$id")) {
        addVersionHeaders(idempotencyKey, expectedVersion)
        setBody(jsonRequestBody(input))
    }.requireBusinessFinanceData()

    override suspend fun recordFinanceEntry(
        id: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): BusinessFinanceEntryDto = httpClient.post(url("$ACCOUNTING_ENTRIES_PATH/$id/record")) {
        addVersionHeaders(idempotencyKey, expectedVersion)
    }.requireBusinessFinanceData()

    override suspend fun voidFinanceEntry(
        id: String,
        expectedVersion: Int,
        reason: String,
        idempotencyKey: String,
    ): BusinessFinanceEntryDto = httpClient.post(url("$ACCOUNTING_ENTRIES_PATH/$id/void")) {
        addVersionHeaders(idempotencyKey, expectedVersion)
        setBody(jsonRequestBody(VoidFinanceEntryRequestDto(reason)))
    }.requireBusinessFinanceData()

    override suspend fun financeOverview(period: String): FinanceOverviewDto =
        httpClient.get(url("$ACCOUNTING_OVERVIEW_PATH?period=${period.encodeURLParameter()}"))
            .requireBusinessFinanceData()

    private fun url(path: String): String = "${supportDeskBaseUrl()}$path"

    private fun io.ktor.client.request.HttpRequestBuilder.addIdempotencyKey(value: String) {
        headers.append(IDEMPOTENCY_KEY_HEADER, value.requireIdempotencyKey())
    }

    private fun io.ktor.client.request.HttpRequestBuilder.addVersionHeaders(
        idempotencyKey: String,
        expectedVersion: Int,
    ) {
        addIdempotencyKey(idempotencyKey)
        require(expectedVersion > 0) { "Expected version must be positive" }
        headers.append(IF_MATCH_HEADER, expectedVersion.toString())
    }
}

class BusinessFinanceRemoteHttpException(
    val statusCode: Int,
    message: String,
) : IllegalStateException(message)

private suspend inline fun <reified T> HttpResponse.requireBusinessFinanceData(): T {
    val payload = bodyAsText()
    if (!status.isSuccess()) {
        val message = runCatching {
            businessFinanceJson.decodeFromString<ApiErrorEnvelope>(payload).message
        }.getOrDefault("El servidor respondiÃ³ con estado ${status.value}.")
        throw BusinessFinanceRemoteHttpException(status.value, message)
    }
    return businessFinanceJson.decodeFromString<ApiEnvelope<T>>(payload).data
}

private fun String.requireIdempotencyKey(): String = trim().also { key ->
    require(key.length in 16..128) { "Idempotency key must contain between 16 and 128 characters" }
}

private val businessFinanceJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

private const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
private const val IF_MATCH_HEADER = "If-Match"
private const val INVOICING_DOCUMENTS_PATH = "/client/business/invoicing/documents"
private const val ACCOUNTING_ENTRIES_PATH = "/client/business/accounting/entries"
private const val ACCOUNTING_OVERVIEW_PATH = "/client/business/accounting/overview"
