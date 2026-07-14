package com.requena.supportdesk.features.invoices.presentation.state

import com.requena.supportdesk.features.invoices.domain.model.Invoice

data class InvoicesUiState(
    val invoices: List<Invoice> = emptyList(),
    val selectedInvoice: Invoice? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)
