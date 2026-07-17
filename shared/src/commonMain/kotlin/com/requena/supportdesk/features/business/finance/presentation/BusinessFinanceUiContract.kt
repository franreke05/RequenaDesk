package com.requena.supportdesk.features.business.finance.presentation

import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.FinanceOverview

data class BusinessInvoicingUiState(
    val documents: List<BusinessSalesDocument> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val accessDenied: Boolean = false,
    val errorMessage: String? = null,
)

data class BusinessAccountingUiState(
    val overview: FinanceOverview? = null,
    val entries: List<BusinessFinanceEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val accessDenied: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface BusinessFinanceUiEvent {
    data object RefreshInvoicing : BusinessFinanceUiEvent
    data object RefreshAccounting : BusinessFinanceUiEvent
    data object CreateSalesDraft : BusinessFinanceUiEvent
    data object CreateFinanceEntry : BusinessFinanceUiEvent
}
