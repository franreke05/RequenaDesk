package com.requena.supportdesk.features.business.finance.presentation

import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.finance.data.repository.BusinessFinanceAccessDeniedException
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceDirection
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceRepository
import com.requena.supportdesk.features.business.finance.domain.BusinessPaymentStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentKind
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentStatus
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.FinanceOverview
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import com.requena.supportdesk.features.business.finance.domain.SalesLineInput
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusinessFinanceViewModelTest {
    @Test
    fun `creating a draft renders the persisted document and supplies an idempotency key`() = runBlocking {
        val repository = DraftCreatingRepository()
        val viewModel = BusinessInvoicingViewModel(
            repository = repository,
            idempotencyKeyFactory = BusinessFinanceIdempotencyKeyFactory { "test-create-key-0001" },
        )

        viewModel.createDraft(draftInput())
        val state = withTimeout(TEST_TIMEOUT_MILLIS) {
            viewModel.state.first { it.documents.singleOrNull()?.id == "document-1" && !it.isSaving }
        }

        assertEquals("test-create-key-0001", repository.idempotencyKey)
        assertEquals("Cliente Uno", state.documents.single().customerName)
        viewModel.clear()
    }

    @Test
    fun `forbidden accounting refresh maps to access denied state and effect`() = runBlocking {
        val viewModel = BusinessAccountingViewModel(ForbiddenAccountingRepository())
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.refresh("2026-07")
        val state = withTimeout(TEST_TIMEOUT_MILLIS) {
            viewModel.state.first { it.accessDenied && !it.isLoading }
        }

        assertTrue(state.errorMessage == null)
        assertEquals(BusinessFinanceUiEffect.AccessDenied, withTimeout(TEST_TIMEOUT_MILLIS) { effect.await() })
        viewModel.clear()
    }
}

private class DraftCreatingRepository : UnsupportedFinanceRepository() {
    var idempotencyKey: String? = null

    override suspend fun createSalesDraft(
        input: SalesDocumentDraftInput,
        idempotencyKey: String,
    ): AppResult<BusinessSalesDocument> {
        this.idempotencyKey = idempotencyKey
        return AppResult.Success(document())
    }
}

private class ForbiddenAccountingRepository : UnsupportedFinanceRepository() {
    override suspend fun listFinanceEntries(): AppResult<List<BusinessFinanceEntry>> =
        AppResult.Error(
            message = "Program authorization is required",
            cause = BusinessFinanceAccessDeniedException("Program authorization is required", IllegalStateException()),
        )
}

private open class UnsupportedFinanceRepository : BusinessFinanceRepository {
    override suspend fun listSalesDocuments(): AppResult<List<BusinessSalesDocument>> = unsupported()
    override suspend fun createSalesDraft(input: SalesDocumentDraftInput, idempotencyKey: String): AppResult<BusinessSalesDocument> = unsupported()
    override suspend fun updateSalesDraft(id: String, expectedVersion: Int, input: SalesDocumentDraftInput, idempotencyKey: String): AppResult<BusinessSalesDocument> = unsupported()
    override suspend fun archiveSalesDocument(id: String, expectedVersion: Int, idempotencyKey: String): AppResult<BusinessSalesDocument> = unsupported()
    override suspend fun listFinanceEntries(): AppResult<List<BusinessFinanceEntry>> = unsupported()
    override suspend fun createFinanceEntry(input: FinanceEntryInput, idempotencyKey: String): AppResult<BusinessFinanceEntry> = unsupported()
    override suspend fun updateFinanceEntry(id: String, expectedVersion: Int, input: FinanceEntryInput, idempotencyKey: String): AppResult<BusinessFinanceEntry> = unsupported()
    override suspend fun recordFinanceEntry(id: String, expectedVersion: Int, idempotencyKey: String): AppResult<BusinessFinanceEntry> = unsupported()
    override suspend fun voidFinanceEntry(id: String, expectedVersion: Int, reason: String, idempotencyKey: String): AppResult<BusinessFinanceEntry> = unsupported()
    override suspend fun financeOverview(period: String): AppResult<FinanceOverview> = unsupported()

    protected fun <T> unsupported(): T = error("Unexpected repository invocation")
}

private fun draftInput() = SalesDocumentDraftInput(
    issuerName = "Estudio Norte",
    customerName = "Cliente Uno",
    issueDate = "2026-07-17",
    lines = listOf(SalesLineInput("ConsultorÃ­a", 1_000, 2_000)),
)

private fun document() = BusinessSalesDocument(
    id = "document-1",
    kind = BusinessSalesDocumentKind.DRAFT_INVOICE,
    status = BusinessSalesDocumentStatus.DRAFT,
    issuerName = "Estudio Norte",
    customerName = "Cliente Uno",
    issueDate = "2026-07-17",
    subtotalCents = 2_000,
    taxCents = 0,
    totalCents = 2_000,
    version = 1,
    lines = emptyList(),
)

private const val TEST_TIMEOUT_MILLIS = 5_000L
