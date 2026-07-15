package com.requena.supportdesk.features.invoices.data.repository

import com.requena.supportdesk.features.invoices.data.datasource.InvoicesDataSource
import com.requena.supportdesk.features.invoices.data.dto.CreateInvoiceItemRequestDto
import com.requena.supportdesk.features.invoices.data.dto.CreateInvoiceRequestDto
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.repository.InvoicesRepository

class InvoicesRepositoryImpl(
    private val dataSource: InvoicesDataSource,
) : InvoicesRepository {

    override fun buildGeneratedInvoiceUrl(input: CreateInvoiceInput): String {
        require(input.clientId.isNotBlank()) { "Selecciona un cliente para generar la factura." }
        require(input.items.isNotEmpty()) { "Agrega al menos un item a la factura." }
        return dataSource.buildGeneratedInvoiceUrl(
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
        )
    }
}
