package com.requena.supportdesk.server.business.finance

import com.requena.supportdesk.features.business.finance.domain.BUSINESS_ACCOUNTING
import com.requena.supportdesk.features.business.finance.domain.BUSINESS_INVOICING
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentStatus
import com.requena.supportdesk.features.business.finance.domain.CalculatedFinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.FinanceOverview
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInputWithTotals

data class BusinessFinanceClientIdentity(
    val userId: String,
    val clientId: String,
)

fun interface BusinessFinanceAccessGuard {
    /** The integrator must delegate to the V8 ACTIVE product subscription. */
    fun hasActiveEntitlement(identity: BusinessFinanceClientIdentity, productKey: String): Boolean
}

class BusinessFinanceForbiddenException(message: String = "This beta program requires administrator authorization") : RuntimeException(message)
class BusinessFinanceNotFoundException(message: String = "Business record was not found") : RuntimeException(message)
class BusinessFinanceConflictException(message: String) : RuntimeException(message)

data class BusinessFinanceAuditEvent(
    val clientId: String,
    val aggregateType: String,
    val aggregateId: String,
    val action: String,
    val actorUserId: String,
    val version: Int,
)

interface BusinessFinanceStore {
    fun listSalesDocuments(clientId: String): List<BusinessSalesDocument>
    fun createSalesDocument(
        clientId: String,
        actorUserId: String,
        draft: SalesDocumentDraftInputWithTotals,
    ): BusinessSalesDocument

    fun updateSalesDocument(
        clientId: String,
        actorUserId: String,
        documentId: String,
        expectedVersion: Int,
        draft: SalesDocumentDraftInputWithTotals,
    ): BusinessSalesDocument

    fun archiveSalesDocument(
        clientId: String,
        actorUserId: String,
        documentId: String,
        expectedVersion: Int,
    ): BusinessSalesDocument

    fun listFinanceEntries(clientId: String): List<BusinessFinanceEntry>
    fun createFinanceEntry(
        clientId: String,
        actorUserId: String,
        entry: CalculatedFinanceEntryInput,
    ): BusinessFinanceEntry

    fun updateFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
        entry: CalculatedFinanceEntryInput,
    ): BusinessFinanceEntry

    fun recordFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
    ): BusinessFinanceEntry

    fun voidFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
        reason: String,
    ): BusinessFinanceEntry

    fun financeOverview(clientId: String, period: String): FinanceOverview
    fun auditEvents(clientId: String): List<BusinessFinanceAuditEvent>
}

internal fun requireInvoicingEntitlement(
    accessGuard: BusinessFinanceAccessGuard,
    identity: BusinessFinanceClientIdentity,
) {
    if (!accessGuard.hasActiveEntitlement(identity, BUSINESS_INVOICING)) throw BusinessFinanceForbiddenException()
}

internal fun requireAccountingEntitlement(
    accessGuard: BusinessFinanceAccessGuard,
    identity: BusinessFinanceClientIdentity,
) {
    if (!accessGuard.hasActiveEntitlement(identity, BUSINESS_ACCOUNTING)) throw BusinessFinanceForbiddenException()
}

internal fun requireDraft(status: BusinessSalesDocumentStatus) {
    if (status != BusinessSalesDocumentStatus.DRAFT) throw BusinessFinanceConflictException("Only drafts can be archived")
}

internal fun requireDraftEntry(status: BusinessFinanceEntryStatus) {
    if (status != BusinessFinanceEntryStatus.DRAFT) throw BusinessFinanceConflictException("Only draft entries can be recorded")
}
