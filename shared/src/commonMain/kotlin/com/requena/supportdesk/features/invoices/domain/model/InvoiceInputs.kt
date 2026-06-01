package com.requena.supportdesk.features.invoices.domain.model

data class CreateInvoiceItemInput(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val sortOrder: Int,
)

data class CreateInvoiceInput(
    val clientId: String,
    val issuedAt: String,
    val dueAt: String?,
    val notes: String?,
    val taxPercent: Double,
    val items: List<CreateInvoiceItemInput>,
)
