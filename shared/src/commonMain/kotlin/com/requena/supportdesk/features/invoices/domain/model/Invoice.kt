package com.requena.supportdesk.features.invoices.domain.model

enum class InvoiceStatus { DRAFT, SENT, PAID, CANCELLED }

data class InvoiceItem(
    val id: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val sortOrder: Int,
) {
    val subtotal: Double get() = quantity * unitPrice
}

data class Invoice(
    val id: String,
    val invoiceNumber: String,
    val clientId: String,
    val clientName: String,
    val status: InvoiceStatus,
    val issuedAt: String,
    val dueAt: String?,
    val notes: String?,
    val taxPercent: Double,
    val items: List<InvoiceItem>,
    val createdAt: String,
    val sentAt: String?,
    val paidAt: String?,
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }
    val taxAmount: Double get() = subtotal * (taxPercent / 100.0)
    val total: Double get() = subtotal + taxAmount
}
