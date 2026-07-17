package com.requena.supportdesk.features.business.finance.domain

import com.requena.supportdesk.core.result.AppResult

/**
 * Client-side contract. The concrete Ktor datasource is deliberately left to
 * the integration layer so the feature can be wired into the existing client
 * token and API envelope configuration without duplicating it.
 */
interface BusinessFinanceRepository {
    suspend fun listSalesDocuments(): AppResult<List<BusinessSalesDocument>>
    suspend fun createSalesDraft(input: SalesDocumentDraftInput, idempotencyKey: String): AppResult<BusinessSalesDocument>
    suspend fun updateSalesDraft(
        id: String,
        expectedVersion: Int,
        input: SalesDocumentDraftInput,
        idempotencyKey: String,
    ): AppResult<BusinessSalesDocument>
    suspend fun archiveSalesDocument(id: String, expectedVersion: Int, idempotencyKey: String): AppResult<BusinessSalesDocument>
    suspend fun listFinanceEntries(): AppResult<List<BusinessFinanceEntry>>
    suspend fun createFinanceEntry(input: FinanceEntryInput, idempotencyKey: String): AppResult<BusinessFinanceEntry>
    suspend fun updateFinanceEntry(
        id: String,
        expectedVersion: Int,
        input: FinanceEntryInput,
        idempotencyKey: String,
    ): AppResult<BusinessFinanceEntry>
    suspend fun recordFinanceEntry(id: String, expectedVersion: Int, idempotencyKey: String): AppResult<BusinessFinanceEntry>
    suspend fun voidFinanceEntry(
        id: String,
        expectedVersion: Int,
        reason: String,
        idempotencyKey: String,
    ): AppResult<BusinessFinanceEntry>
    suspend fun financeOverview(period: String): AppResult<FinanceOverview>
}
