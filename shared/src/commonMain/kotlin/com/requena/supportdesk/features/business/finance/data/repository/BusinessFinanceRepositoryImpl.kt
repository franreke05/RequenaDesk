package com.requena.supportdesk.features.business.finance.data.repository

import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.business.finance.data.datasource.BusinessFinanceDataSource
import com.requena.supportdesk.features.business.finance.data.datasource.BusinessFinanceRemoteHttpException
import com.requena.supportdesk.features.business.finance.data.mapper.BusinessFinanceMapper
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceRepository
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.FinanceOverview
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import kotlinx.coroutines.CancellationException

class BusinessFinanceRepositoryImpl(
    private val dataSource: BusinessFinanceDataSource,
) : BusinessFinanceRepository {
    override suspend fun listSalesDocuments(): AppResult<List<BusinessSalesDocument>> =
        request("No se pudieron cargar los borradores de facturaciÃ³n.") {
            dataSource.listSalesDocuments().map(BusinessFinanceMapper::salesDocument)
        }

    override suspend fun createSalesDraft(
        input: SalesDocumentDraftInput,
        idempotencyKey: String,
    ): AppResult<BusinessSalesDocument> = request("No se pudo crear el borrador.") {
        BusinessFinanceMapper.salesDocument(
            dataSource.createSalesDraft(BusinessFinanceMapper.salesDocumentRequest(input), idempotencyKey),
        )
    }

    override suspend fun updateSalesDraft(
        id: String,
        expectedVersion: Int,
        input: SalesDocumentDraftInput,
        idempotencyKey: String,
    ): AppResult<BusinessSalesDocument> = request("No se pudo actualizar el borrador.") {
        BusinessFinanceMapper.salesDocument(
            dataSource.updateSalesDraft(
                id = id,
                expectedVersion = expectedVersion,
                input = BusinessFinanceMapper.salesDocumentRequest(input),
                idempotencyKey = idempotencyKey,
            ),
        )
    }

    override suspend fun archiveSalesDocument(
        id: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): AppResult<BusinessSalesDocument> = request("No se pudo archivar el borrador.") {
        BusinessFinanceMapper.salesDocument(
            dataSource.archiveSalesDocument(id, expectedVersion, idempotencyKey),
        )
    }

    override suspend fun listFinanceEntries(): AppResult<List<BusinessFinanceEntry>> =
        request("No se pudieron cargar los registros contables.") {
            dataSource.listFinanceEntries().map(BusinessFinanceMapper::financeEntry)
        }

    override suspend fun createFinanceEntry(
        input: FinanceEntryInput,
        idempotencyKey: String,
    ): AppResult<BusinessFinanceEntry> = request("No se pudo crear el registro.") {
        BusinessFinanceMapper.financeEntry(
            dataSource.createFinanceEntry(BusinessFinanceMapper.financeEntryRequest(input), idempotencyKey),
        )
    }

    override suspend fun updateFinanceEntry(
        id: String,
        expectedVersion: Int,
        input: FinanceEntryInput,
        idempotencyKey: String,
    ): AppResult<BusinessFinanceEntry> = request("No se pudo actualizar el registro.") {
        BusinessFinanceMapper.financeEntry(
            dataSource.updateFinanceEntry(
                id = id,
                expectedVersion = expectedVersion,
                input = BusinessFinanceMapper.financeEntryRequest(input),
                idempotencyKey = idempotencyKey,
            ),
        )
    }

    override suspend fun recordFinanceEntry(
        id: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): AppResult<BusinessFinanceEntry> = request("No se pudo registrar el asiento.") {
        BusinessFinanceMapper.financeEntry(dataSource.recordFinanceEntry(id, expectedVersion, idempotencyKey))
    }

    override suspend fun voidFinanceEntry(
        id: String,
        expectedVersion: Int,
        reason: String,
        idempotencyKey: String,
    ): AppResult<BusinessFinanceEntry> = request("No se pudo anular el asiento.") {
        BusinessFinanceMapper.financeEntry(
            dataSource.voidFinanceEntry(id, expectedVersion, reason, idempotencyKey),
        )
    }

    override suspend fun financeOverview(period: String): AppResult<FinanceOverview> =
        request("No se pudo cargar el resumen contable.") {
            BusinessFinanceMapper.financeOverview(dataSource.financeOverview(period))
        }

    private suspend fun <T> request(
        fallbackMessage: String,
        block: suspend () -> T,
    ): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: BusinessFinanceRemoteHttpException) {
        val cause = if (error.statusCode == ACCESS_DENIED_STATUS) {
            BusinessFinanceAccessDeniedException(error.message ?: fallbackMessage, error)
        } else {
            error
        }
        AppResult.Error(error.message ?: fallbackMessage, cause)
    } catch (error: Throwable) {
        AppResult.Error(error.message ?: fallbackMessage, error)
    }
}

class BusinessFinanceAccessDeniedException(
    message: String,
    cause: Throwable,
) : IllegalStateException(message, cause)

private const val ACCESS_DENIED_STATUS = 403
