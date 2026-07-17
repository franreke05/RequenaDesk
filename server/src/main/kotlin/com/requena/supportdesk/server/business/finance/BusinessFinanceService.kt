package com.requena.supportdesk.server.business.finance

import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceCalculator
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.FinanceOverview
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput

/**
 * Server-authoritative vertical. It deliberately has no dependency on
 * SupportDeskService; the application module supplies the V8 entitlement guard
 * and registers the public Ktor route later.
 */
class BusinessFinanceService(
    private val store: BusinessFinanceStore,
    private val accessGuard: BusinessFinanceAccessGuard,
) {
    fun listSalesDocuments(identity: BusinessFinanceClientIdentity): List<BusinessSalesDocument> {
        requireInvoicingEntitlement(accessGuard, identity)
        return store.listSalesDocuments(identity.clientId)
    }

    fun createSalesDraft(
        identity: BusinessFinanceClientIdentity,
        input: SalesDocumentDraftInput,
    ): BusinessSalesDocument {
        requireInvoicingEntitlement(accessGuard, identity)
        return store.createSalesDocument(identity.clientId, identity.userId, BusinessFinanceCalculator.calculateSalesDocument(input))
    }

    fun archiveSalesDocument(
        identity: BusinessFinanceClientIdentity,
        documentId: String,
        expectedVersion: Int,
    ): BusinessSalesDocument {
        requireInvoicingEntitlement(accessGuard, identity)
        return store.archiveSalesDocument(identity.clientId, identity.userId, documentId, expectedVersion)
    }

    fun updateSalesDraft(
        identity: BusinessFinanceClientIdentity,
        documentId: String,
        expectedVersion: Int,
        input: SalesDocumentDraftInput,
    ): BusinessSalesDocument {
        requireInvoicingEntitlement(accessGuard, identity)
        return store.updateSalesDocument(
            identity.clientId,
            identity.userId,
            documentId,
            expectedVersion,
            BusinessFinanceCalculator.calculateSalesDocument(input),
        )
    }

    fun listFinanceEntries(identity: BusinessFinanceClientIdentity): List<BusinessFinanceEntry> {
        requireAccountingEntitlement(accessGuard, identity)
        return store.listFinanceEntries(identity.clientId)
    }

    fun createFinanceEntry(
        identity: BusinessFinanceClientIdentity,
        input: FinanceEntryInput,
    ): BusinessFinanceEntry {
        requireAccountingEntitlement(accessGuard, identity)
        return store.createFinanceEntry(identity.clientId, identity.userId, BusinessFinanceCalculator.calculateFinanceEntry(input))
    }

    fun recordFinanceEntry(
        identity: BusinessFinanceClientIdentity,
        entryId: String,
        expectedVersion: Int,
    ): BusinessFinanceEntry {
        requireAccountingEntitlement(accessGuard, identity)
        return store.recordFinanceEntry(identity.clientId, identity.userId, entryId, expectedVersion)
    }

    fun updateFinanceEntry(
        identity: BusinessFinanceClientIdentity,
        entryId: String,
        expectedVersion: Int,
        input: FinanceEntryInput,
    ): BusinessFinanceEntry {
        requireAccountingEntitlement(accessGuard, identity)
        return store.updateFinanceEntry(
            identity.clientId,
            identity.userId,
            entryId,
            expectedVersion,
            BusinessFinanceCalculator.calculateFinanceEntry(input),
        )
    }

    fun voidFinanceEntry(
        identity: BusinessFinanceClientIdentity,
        entryId: String,
        expectedVersion: Int,
        reason: String,
    ): BusinessFinanceEntry {
        requireAccountingEntitlement(accessGuard, identity)
        if (reason.trim().isEmpty() || reason.trim().length > 500) {
            throw com.requena.supportdesk.features.business.finance.domain.BusinessFinanceValidationException("Void reason is required")
        }
        return store.voidFinanceEntry(identity.clientId, identity.userId, entryId, expectedVersion, reason.trim())
    }

    fun financeOverview(identity: BusinessFinanceClientIdentity, period: String): FinanceOverview {
        requireAccountingEntitlement(accessGuard, identity)
        if (!Regex("\\d{4}-\\d{2}").matches(period)) {
            throw com.requena.supportdesk.features.business.finance.domain.BusinessFinanceValidationException("Period must use YYYY-MM")
        }
        return store.financeOverview(identity.clientId, period)
    }
}
