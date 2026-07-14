package com.requena.supportdesk.features.invoices.presentation.effect

sealed interface InvoicesUiEffect {
    data class ShowMessage(val message: String) : InvoicesUiEffect
    data class OpenPdfUrl(val url: String) : InvoicesUiEffect
    data class InvoiceCreated(val invoiceId: String) : InvoicesUiEffect
}
