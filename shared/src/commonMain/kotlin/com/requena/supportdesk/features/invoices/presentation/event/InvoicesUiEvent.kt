package com.requena.supportdesk.features.invoices.presentation.event

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput

sealed interface InvoicesUiEvent {
    data object RefreshSavedInvoices : InvoicesUiEvent
    data class OpenSavedInvoice(val fileName: String) : InvoicesUiEvent
    data class DeleteSavedInvoice(val fileName: String) : InvoicesUiEvent
    data class GenerateInvoice(val input: CreateInvoiceInput) : InvoicesUiEvent
}
