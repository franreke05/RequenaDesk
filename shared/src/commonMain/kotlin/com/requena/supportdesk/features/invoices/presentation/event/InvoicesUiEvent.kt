package com.requena.supportdesk.features.invoices.presentation.event

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceStatus

sealed interface InvoicesUiEvent {
    object Load : InvoicesUiEvent
    data class SelectInvoice(val invoiceId: String) : InvoicesUiEvent
    data class CreateInvoice(val input: CreateInvoiceInput) : InvoicesUiEvent
    data class UpdateStatus(val invoiceId: String, val status: InvoiceStatus) : InvoicesUiEvent
    data class DownloadPdf(val invoiceId: String) : InvoicesUiEvent
}
