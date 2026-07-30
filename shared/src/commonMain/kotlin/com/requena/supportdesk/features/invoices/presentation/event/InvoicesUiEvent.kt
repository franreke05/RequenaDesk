package com.requena.supportdesk.features.invoices.presentation.event

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput

sealed interface InvoicesUiEvent {
    data object RefreshSavedInvoices : InvoicesUiEvent
    data class OpenSavedInvoice(val fileName: String) : InvoicesUiEvent
    data class DeleteSavedInvoice(val fileName: String) : InvoicesUiEvent

    /** [selectedLogIds] are the TaskLog ids backing every TASK_HOURS line in [input]. */
    data class GenerateInvoice(val input: CreateInvoiceInput, val selectedLogIds: List<String> = emptyList()) : InvoicesUiEvent

    /** Distinct from DeleteSavedInvoice: releases the ledger's billed units back to pending. */
    data class VoidInvoice(val fileName: String, val reason: String? = null) : InvoicesUiEvent
}
