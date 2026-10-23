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
    val issuer: InvoiceIssuer = InvoiceIssuer(),
    val recipient: InvoiceRecipient = InvoiceRecipient(legalName = clientName),
    /** Retención de IRPF sobre la base imponible (0 si no aplica; 15 general, 7 nuevos autónomos). */
    val withholdingPercent: Double = 0.0,
    /** Fecha o periodo de realización del servicio, solo si es distinto de la fecha de emisión. */
    val operationDate: String? = null,
    /** Mención legal obligatoria cuando el IVA es 0 % (exención o inversión del sujeto pasivo). */
    val taxExemptionNote: String? = null,
)
