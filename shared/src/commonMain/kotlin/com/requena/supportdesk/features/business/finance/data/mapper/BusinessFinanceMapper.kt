package com.requena.supportdesk.features.business.finance.data.mapper

import com.requena.supportdesk.features.business.finance.data.dto.BusinessFinanceEntryDto
import com.requena.supportdesk.features.business.finance.data.dto.BusinessSalesDocumentDto
import com.requena.supportdesk.features.business.finance.data.dto.CalculatedSalesLineDto
import com.requena.supportdesk.features.business.finance.data.dto.FinanceEntryRequestDto
import com.requena.supportdesk.features.business.finance.data.dto.FinanceOverviewDto
import com.requena.supportdesk.features.business.finance.data.dto.SalesDocumentDraftRequestDto
import com.requena.supportdesk.features.business.finance.data.dto.SalesLineRequestDto
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.CalculatedSalesLine
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.FinanceOverview
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import com.requena.supportdesk.features.business.finance.domain.SalesLineInput

object BusinessFinanceMapper {
    fun salesDocumentRequest(input: SalesDocumentDraftInput): SalesDocumentDraftRequestDto =
        SalesDocumentDraftRequestDto(
            kind = input.kind,
            issuerName = input.issuerName,
            customerName = input.customerName,
            issueDate = input.issueDate,
            dueDate = input.dueDate,
            notes = input.notes,
            currency = input.currency,
            lines = input.lines.map(::salesLineRequest),
        )

    fun salesDocument(dto: BusinessSalesDocumentDto): BusinessSalesDocument =
        BusinessSalesDocument(
            id = dto.id,
            kind = dto.kind,
            status = dto.status,
            issuerName = dto.issuerName,
            customerName = dto.customerName,
            issueDate = dto.issueDate,
            dueDate = dto.dueDate,
            notes = dto.notes,
            currency = dto.currency,
            subtotalCents = dto.subtotalCents,
            taxCents = dto.taxCents,
            totalCents = dto.totalCents,
            version = dto.version,
            lines = dto.lines.map(::calculatedSalesLine),
            betaDisclaimer = dto.betaDisclaimer,
        )

    fun financeEntryRequest(input: FinanceEntryInput): FinanceEntryRequestDto =
        FinanceEntryRequestDto(
            direction = input.direction,
            occurredOn = input.occurredOn,
            description = input.description,
            netCents = input.netCents,
            taxRateBasisPoints = input.taxRateBasisPoints,
            counterpartyName = input.counterpartyName,
            categoryName = input.categoryName,
            paymentStatus = input.paymentStatus,
            externalReference = input.externalReference,
            currency = input.currency,
        )

    fun financeEntry(dto: BusinessFinanceEntryDto): BusinessFinanceEntry =
        BusinessFinanceEntry(
            id = dto.id,
            direction = dto.direction,
            status = dto.status,
            occurredOn = dto.occurredOn,
            description = dto.description,
            netCents = dto.netCents,
            taxRateBasisPoints = dto.taxRateBasisPoints,
            taxCents = dto.taxCents,
            grossCents = dto.grossCents,
            counterpartyName = dto.counterpartyName,
            categoryName = dto.categoryName,
            paymentStatus = dto.paymentStatus,
            externalReference = dto.externalReference,
            currency = dto.currency,
            voidReason = dto.voidReason,
            version = dto.version,
        )

    fun financeOverview(dto: FinanceOverviewDto): FinanceOverview =
        FinanceOverview(
            period = dto.period,
            incomeCents = dto.incomeCents,
            expenseCents = dto.expenseCents,
            netCashFlowCents = dto.netCashFlowCents,
            pendingCents = dto.pendingCents,
            currency = dto.currency,
            isInformationalOnly = dto.isInformationalOnly,
        )

    private fun salesLineRequest(input: SalesLineInput): SalesLineRequestDto =
        SalesLineRequestDto(
            description = input.description,
            quantityMilli = input.quantityMilli,
            unitPriceCents = input.unitPriceCents,
            taxRateBasisPoints = input.taxRateBasisPoints,
            discountBasisPoints = input.discountBasisPoints,
        )

    private fun calculatedSalesLine(dto: CalculatedSalesLineDto): CalculatedSalesLine =
        CalculatedSalesLine(
            description = dto.description,
            quantityMilli = dto.quantityMilli,
            unitPriceCents = dto.unitPriceCents,
            taxRateBasisPoints = dto.taxRateBasisPoints,
            discountBasisPoints = dto.discountBasisPoints,
            subtotalCents = dto.subtotalCents,
            taxCents = dto.taxCents,
            totalCents = dto.totalCents,
        )
}
