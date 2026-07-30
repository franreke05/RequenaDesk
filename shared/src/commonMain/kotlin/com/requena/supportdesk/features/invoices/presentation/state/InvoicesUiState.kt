package com.requena.supportdesk.features.invoices.presentation.state

import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile

data class InvoicesUiState(
    val savedInvoices: List<InvoicePdfFile> = emptyList(),
    val isLoadingSavedInvoices: Boolean = false,
    val isGenerating: Boolean = false,
    val deletingInvoiceFileName: String? = null,
    val voidingInvoiceFileName: String? = null,
    // TaskLog ids currently billed under a non-voided invoice - drives which pending units the
    // invoice-creation tree may still offer, and which saved invoices may still be voided.
    val billedLogIds: Set<String> = emptySet(),
    val voidedInvoiceNumbers: Set<String> = emptySet(),
    val errorMessage: String? = null,
) {
    fun isVoided(invoice: InvoicePdfFile): Boolean = invoice.fileName.substringBefore("_") in voidedInvoiceNumbers
}
