package com.requena.supportdesk.features.invoices.presentation.event

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput

sealed interface InvoicesUiEvent {
    data class GenerateInvoice(val input: CreateInvoiceInput) : InvoicesUiEvent
}
