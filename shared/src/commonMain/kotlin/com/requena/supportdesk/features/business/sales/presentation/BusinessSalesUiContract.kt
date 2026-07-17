package com.requena.supportdesk.features.business.sales.presentation

import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItem
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessQuote
import com.requena.supportdesk.features.business.sales.domain.BusinessSale
import com.requena.supportdesk.features.business.sales.domain.BusinessStockSummary

data class BusinessCustomersUiState(
    val customers: List<BusinessCustomer> = emptyList(),
    val selectedCustomer: BusinessCustomerDetail? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class BusinessCatalogUiState(
    val items: List<BusinessCatalogItem> = emptyList(),
    val stock: List<BusinessStockSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class BusinessQuotesUiState(
    val quotes: List<BusinessQuote> = emptyList(),
    val selectedQuote: BusinessQuote? = null,
    val sales: List<BusinessSale> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface BusinessSalesUiEffect {
    data class ShowMessage(val message: String) : BusinessSalesUiEffect
}
