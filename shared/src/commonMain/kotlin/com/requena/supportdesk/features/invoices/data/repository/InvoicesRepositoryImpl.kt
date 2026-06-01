package com.requena.supportdesk.features.invoices.data.repository

import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.invoices.data.datasource.InvoicesDataSource
import com.requena.supportdesk.features.invoices.data.dto.CreateInvoiceItemRequestDto
import com.requena.supportdesk.features.invoices.data.dto.CreateInvoiceRequestDto
import com.requena.supportdesk.features.invoices.data.dto.UpdateInvoiceStatusRequestDto
import com.requena.supportdesk.features.invoices.data.mapper.InvoicesMapper
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.Invoice
import com.requena.supportdesk.features.invoices.domain.model.InvoiceStatus
import com.requena.supportdesk.features.invoices.domain.repository.InvoicesRepository

class InvoicesRepositoryImpl(
    private val dataSource: InvoicesDataSource,
) : InvoicesRepository {

    override suspend fun getInvoices(): AppResult<List<Invoice>> = runCatching {
        dataSource.getInvoices().map(InvoicesMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudieron cargar las facturas.", cause = it) },
    )

    override suspend fun getInvoice(id: String): AppResult<Invoice> = runCatching {
        dataSource.getInvoice(id)?.let(InvoicesMapper::fromDto)
            ?: error("Factura no encontrada.")
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo cargar la factura.", cause = it) },
    )

    override suspend fun createInvoice(input: CreateInvoiceInput): AppResult<Invoice> = runCatching {
        dataSource.createInvoice(
            CreateInvoiceRequestDto(
                clientId = input.clientId,
                issuedAt = input.issuedAt,
                dueAt = input.dueAt,
                notes = input.notes?.ifBlank { null },
                taxPercent = input.taxPercent,
                items = input.items.map {
                    CreateInvoiceItemRequestDto(
                        description = it.description,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        sortOrder = it.sortOrder,
                    )
                },
            ),
        ).let(InvoicesMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo crear la factura.", cause = it) },
    )

    override suspend fun updateStatus(invoiceId: String, status: InvoiceStatus): AppResult<Invoice> = runCatching {
        dataSource.updateStatus(invoiceId, UpdateInvoiceStatusRequestDto(status.name)).let(InvoicesMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo actualizar el estado.", cause = it) },
    )

    override suspend fun getPdfUrl(invoiceId: String): AppResult<String> = runCatching {
        dataSource.getPdfUrl(invoiceId).url
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo obtener el PDF.", cause = it) },
    )
}
