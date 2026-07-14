package com.requena.supportdesk.features.invoices.data.mapper

import com.requena.supportdesk.features.invoices.data.dto.InvoiceDto
import com.requena.supportdesk.features.invoices.data.dto.InvoiceItemDto
import com.requena.supportdesk.features.invoices.domain.model.Invoice
import com.requena.supportdesk.features.invoices.domain.model.InvoiceItem
import com.requena.supportdesk.features.invoices.domain.model.InvoiceStatus

object InvoicesMapper {
    fun fromDto(dto: InvoiceDto): Invoice = Invoice(
        id = dto.id,
        invoiceNumber = dto.invoiceNumber,
        clientId = dto.clientId,
        clientName = dto.clientName,
        status = runCatching { InvoiceStatus.valueOf(dto.status) }.getOrDefault(InvoiceStatus.DRAFT),
        issuedAt = dto.issuedAt,
        dueAt = dto.dueAt,
        notes = dto.notes,
        taxPercent = dto.taxPercent,
        items = dto.items.map(::itemFromDto),
        createdAt = dto.createdAt,
        sentAt = dto.sentAt,
        paidAt = dto.paidAt,
    )

    fun itemFromDto(dto: InvoiceItemDto): InvoiceItem = InvoiceItem(
        id = dto.id,
        description = dto.description,
        quantity = dto.quantity,
        unitPrice = dto.unitPrice,
        sortOrder = dto.sortOrder,
    )
}
