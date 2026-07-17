package com.requena.supportdesk.features.invoices.domain.model

enum class InvoiceItemKind {
    TASK_HOURS,
    ACTIVITY,
}

data class CreateInvoiceItemInput(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val sortOrder: Int,
    val kind: InvoiceItemKind = InvoiceItemKind.TASK_HOURS,
    val detail: String? = null,
)

data class CreateInvoiceInput(
    val clientId: String,
    val clientName: String,
    val issuedAt: String,
    val dueAt: String?,
    val notes: String?,
    val taxPercent: Double,
    val items: List<CreateInvoiceItemInput>,
    val reference: String? = null,
)
