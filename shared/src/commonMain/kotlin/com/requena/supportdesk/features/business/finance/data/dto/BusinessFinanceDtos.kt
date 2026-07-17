package com.requena.supportdesk.features.business.finance.data.dto

import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceDirection
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessPaymentStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentKind
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentStatus
import com.requena.supportdesk.features.business.finance.domain.FINANCE_BETA_DISCLAIMER
import kotlinx.serialization.Serializable

@Serializable
data class SalesLineRequestDto(
    val description: String,
    val quantityMilli: Long,
    val unitPriceCents: Long,
    val taxRateBasisPoints: Int = 0,
    val discountBasisPoints: Int = 0,
)

@Serializable
data class SalesDocumentDraftRequestDto(
    val kind: BusinessSalesDocumentKind = BusinessSalesDocumentKind.DRAFT_INVOICE,
    val issuerName: String,
    val customerName: String,
    val issueDate: String,
    val dueDate: String? = null,
    val notes: String? = null,
    val currency: String = "EUR",
    val lines: List<SalesLineRequestDto>,
)

@Serializable
data class CalculatedSalesLineDto(
    val description: String,
    val quantityMilli: Long,
    val unitPriceCents: Long,
    val taxRateBasisPoints: Int,
    val discountBasisPoints: Int,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

@Serializable
data class BusinessSalesDocumentDto(
    val id: String,
    val kind: BusinessSalesDocumentKind,
    val status: BusinessSalesDocumentStatus,
    val issuerName: String,
    val customerName: String,
    val issueDate: String,
    val dueDate: String? = null,
    val notes: String? = null,
    val currency: String = "EUR",
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val version: Int,
    val lines: List<CalculatedSalesLineDto> = emptyList(),
    val betaDisclaimer: String = FINANCE_BETA_DISCLAIMER,
)

@Serializable
data class FinanceEntryRequestDto(
    val direction: BusinessFinanceDirection,
    val occurredOn: String,
    val description: String,
    val netCents: Long,
    val taxRateBasisPoints: Int = 0,
    val counterpartyName: String? = null,
    val categoryName: String? = null,
    val paymentStatus: BusinessPaymentStatus = BusinessPaymentStatus.PENDING,
    val externalReference: String? = null,
    val currency: String = "EUR",
)

@Serializable
data class BusinessFinanceEntryDto(
    val id: String,
    val direction: BusinessFinanceDirection,
    val status: BusinessFinanceEntryStatus,
    val occurredOn: String,
    val description: String,
    val netCents: Long,
    val taxRateBasisPoints: Int,
    val taxCents: Long,
    val grossCents: Long,
    val counterpartyName: String? = null,
    val categoryName: String? = null,
    val paymentStatus: BusinessPaymentStatus,
    val externalReference: String? = null,
    val currency: String = "EUR",
    val voidReason: String? = null,
    val version: Int,
)

@Serializable
data class FinanceOverviewDto(
    val period: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val netCashFlowCents: Long,
    val pendingCents: Long,
    val currency: String = "EUR",
    val isInformationalOnly: Boolean = true,
)

@Serializable
data class VoidFinanceEntryRequestDto(
    val reason: String,
)
