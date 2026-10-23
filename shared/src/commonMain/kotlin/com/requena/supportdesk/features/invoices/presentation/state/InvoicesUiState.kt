package com.requena.supportdesk.features.invoices.presentation.state

import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.domain.model.InvoiceRecipient

data class InvoicesUiState(
    val savedInvoices: List<InvoicePdfFile> = emptyList(),
    val isLoadingSavedInvoices: Boolean = false,
    val isGenerating: Boolean = false,
    val voidingInvoiceFileName: String? = null,
    // TaskLog ids currently billed under a non-voided invoice - drives which pending units the
    // invoice-creation tree may still offer, and which saved invoices may still be voided.
    val billedLogIds: Set<String> = emptySet(),
    val voidedInvoiceNumbers: Set<String> = emptySet(),
    /** Saved issuer data (the self-employed user); null until loaded from the local store. */
    val issuer: InvoiceIssuer? = null,
    /** Last fiscal data used per client id, loaded on demand. */
    val recipientsByClientId: Map<String, InvoiceRecipient> = emptyMap(),
    val errorMessage: String? = null,
) {
    fun isVoided(invoice: InvoicePdfFile): Boolean = invoice.fileName.substringBefore("_") in voidedInvoiceNumbers
}
