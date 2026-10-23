package com.requena.supportdesk.features.invoices.presentation.event

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput

sealed interface InvoicesUiEvent {
    data object RefreshSavedInvoices : InvoicesUiEvent
    data class OpenSavedInvoice(val fileName: String) : InvoicesUiEvent

    /** Loads the fiscal data last used for [clientId] so the form can prefill it. */
    data class LoadRecipient(val clientId: String) : InvoicesUiEvent

    /**
     * [selectedLogIds] are the TaskLog ids backing every TASK_HOURS line in [input]. The issuer
     * and recipient fiscal data in [input] are remembered locally for the next invoice.
     */
    data class GenerateInvoice(val input: CreateInvoiceInput, val selectedLogIds: List<String> = emptyList()) : InvoicesUiEvent

    /** Marks the invoice as voided and releases its billed time back to pending. The PDF is kept. */
    data class VoidInvoice(val fileName: String, val reason: String? = null) : InvoicesUiEvent
}
