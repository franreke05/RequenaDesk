package com.requena.supportdesk.features.invoices.presentation.state

data class InvoicesUiState(
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
)
