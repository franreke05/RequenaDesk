package com.requena.supportdesk.features.invoices.presentation.state

import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile

data class InvoicesUiState(
    val savedInvoices: List<InvoicePdfFile> = emptyList(),
    val isLoadingSavedInvoices: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
)
