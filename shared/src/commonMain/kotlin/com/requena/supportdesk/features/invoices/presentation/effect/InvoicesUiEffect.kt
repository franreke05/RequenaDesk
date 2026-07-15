package com.requena.supportdesk.features.invoices.presentation.effect

sealed interface InvoicesUiEffect {
    data class ShowMessage(val message: String) : InvoicesUiEffect
    data class OpenGeneratedInvoice(val url: String) : InvoicesUiEffect
}
