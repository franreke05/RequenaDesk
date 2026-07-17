package com.requena.supportdesk.features.business.finance.data.repository

import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.finance.data.datasource.BusinessFinanceDataSource
import com.requena.supportdesk.features.business.finance.data.datasource.BusinessFinanceRemoteHttpException
import com.requena.supportdesk.features.business.finance.data.dto.BusinessFinanceEntryDto
import com.requena.supportdesk.features.business.finance.data.dto.BusinessSalesDocumentDto
import com.requena.supportdesk.features.business.finance.data.dto.FinanceEntryRequestDto
import com.requena.supportdesk.features.business.finance.data.dto.FinanceOverviewDto
import com.requena.supportdesk.features.business.finance.data.dto.SalesDocumentDraftRequestDto
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceDirection
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessPaymentStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentKind
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentStatus
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import com.requena.supportdesk.features.business.finance.domain.SalesLineInput
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusinessFinanceRepositoryImplTest {
    @Test
    fun `forbidden remote responses become access denied repository errors`() = runBlocking {
        val result = BusinessFinanceRepositoryImpl(ForbiddenDataSource()).listSalesDocuments()

        assertTrue(result is AppResult.Error)
        assertTrue(result.cause is BusinessFinanceAccessDeniedException)
        assertEquals("Program authorization is required", result.message)
    }

    @Test
    fun `update forwards version idempotency key and mapped request to remote source`() = runBlocking {
        val source = CapturingDataSource()
        val input = SalesDocumentDraftInput(
            issuerName = "Estudio Norte",
            customerName = "Cliente Uno",
            issueDate = "2026-07-17",
            lines = listOf(SalesLineInput("ConsultorÃ­a", 1_000, 2_000)),
        )

        val result = BusinessFinanceRepositoryImpl(source).updateSalesDraft(
            id = "document-1",
            expectedVersion = 7,
            input = input,
            idempotencyKey = "idempotency-key-0001",
        )

        assertTrue(result is AppResult.Success)
        assertEquals("document-1", source.updatedId)
        assertEquals(7, source.updatedVersion)
        assertEquals("idempotency-key-0001", source.updatedIdempotencyKey)
        assertEquals("Cliente Uno", source.updatedRequest?.customerName)
        assertEquals(2_000, result.data.totalCents)
    }
}

private class ForbiddenDataSource : BusinessFinanceDataSource by UnsupportedDataSource() {
    override suspend fun listSalesDocuments(): List<BusinessSalesDocumentDto> =
        throw BusinessFinanceRemoteHttpException(403, "Program authorization is required")
}

private class CapturingDataSource : BusinessFinanceDataSource by UnsupportedDataSource() {
    var updatedId: String? = null
    var updatedVersion: Int? = null
    var updatedRequest: SalesDocumentDraftRequestDto? = null
    var updatedIdempotencyKey: String? = null

    override suspend fun updateSalesDraft(
        id: String,
        expectedVersion: Int,
        input: SalesDocumentDraftRequestDto,
        idempotencyKey: String,
    ): BusinessSalesDocumentDto {
        updatedId = id
        updatedVersion = expectedVersion
        updatedRequest = input
        updatedIdempotencyKey = idempotencyKey
        return documentDto()
    }
}

private open class UnsupportedDataSource : BusinessFinanceDataSource {
    override suspend fun listSalesDocuments(): List<BusinessSalesDocumentDto> = unsupported()
    override suspend fun createSalesDraft(input: SalesDocumentDraftRequestDto, idempotencyKey: String): BusinessSalesDocumentDto = unsupported()
    override suspend fun updateSalesDraft(id: String, expectedVersion: Int, input: SalesDocumentDraftRequestDto, idempotencyKey: String): BusinessSalesDocumentDto = unsupported()
    override suspend fun archiveSalesDocument(id: String, expectedVersion: Int, idempotencyKey: String): BusinessSalesDocumentDto = unsupported()
    override suspend fun listFinanceEntries(): List<BusinessFinanceEntryDto> = unsupported()
    override suspend fun createFinanceEntry(input: FinanceEntryRequestDto, idempotencyKey: String): BusinessFinanceEntryDto = unsupported()
    override suspend fun updateFinanceEntry(id: String, expectedVersion: Int, input: FinanceEntryRequestDto, idempotencyKey: String): BusinessFinanceEntryDto = unsupported()
    override suspend fun recordFinanceEntry(id: String, expectedVersion: Int, idempotencyKey: String): BusinessFinanceEntryDto = unsupported()
    override suspend fun voidFinanceEntry(id: String, expectedVersion: Int, reason: String, idempotencyKey: String): BusinessFinanceEntryDto = unsupported()
    override suspend fun financeOverview(period: String): FinanceOverviewDto = unsupported()

    protected fun <T> unsupported(): T = error("Unexpected data-source invocation")
}

private fun documentDto() = BusinessSalesDocumentDto(
    id = "document-1",
    kind = BusinessSalesDocumentKind.DRAFT_INVOICE,
    status = BusinessSalesDocumentStatus.DRAFT,
    issuerName = "Estudio Norte",
    customerName = "Cliente Uno",
    issueDate = "2026-07-17",
    subtotalCents = 2_000,
    taxCents = 0,
    totalCents = 2_000,
    version = 8,
)
