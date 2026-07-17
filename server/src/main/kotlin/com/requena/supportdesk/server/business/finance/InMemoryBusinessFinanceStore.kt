package com.requena.supportdesk.server.business.finance

import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntry
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessPaymentStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocument
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentStatus
import com.requena.supportdesk.features.business.finance.domain.CalculatedFinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.FinanceOverview
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInputWithTotals
import java.util.UUID

/**
 * Deterministic-enough store for Ktor tests and local wiring. It intentionally
 * keeps tenant ownership alongside every record so cross-tenant reads fail the
 * same way as the PostgreSQL implementation.
 */
class InMemoryBusinessFinanceStore(
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : BusinessFinanceStore {
    private val lock = Any()
    private val documents = mutableMapOf<String, StoredDocument>()
    private val entries = mutableMapOf<String, StoredEntry>()
    private val events = mutableListOf<BusinessFinanceAuditEvent>()

    override fun listSalesDocuments(clientId: String): List<BusinessSalesDocument> = synchronized(lock) {
        documents.values
            .asSequence()
            .filter { it.clientId == clientId }
            .map(StoredDocument::document)
            .sortedWith(compareByDescending<BusinessSalesDocument> { it.issueDate }.thenByDescending { it.id })
            .toList()
    }

    override fun createSalesDocument(
        clientId: String,
        actorUserId: String,
        draft: SalesDocumentDraftInputWithTotals,
    ): BusinessSalesDocument = synchronized(lock) {
        val document = BusinessSalesDocument(
            id = idFactory(),
            kind = draft.input.kind,
            status = BusinessSalesDocumentStatus.DRAFT,
            issuerName = draft.input.issuerName,
            customerName = draft.input.customerName,
            issueDate = draft.input.issueDate,
            dueDate = draft.input.dueDate,
            notes = draft.input.notes,
            currency = draft.input.currency,
            subtotalCents = draft.subtotalCents,
            taxCents = draft.taxCents,
            totalCents = draft.totalCents,
            version = 1,
            lines = draft.lines,
        )
        documents[document.id] = StoredDocument(clientId, document)
        events += BusinessFinanceAuditEvent(clientId, "SALES_DOCUMENT", document.id, "CREATED", actorUserId, document.version)
        document
    }

    override fun archiveSalesDocument(
        clientId: String,
        actorUserId: String,
        documentId: String,
        expectedVersion: Int,
    ): BusinessSalesDocument = synchronized(lock) {
        val current = documents[documentId]?.takeIf { it.clientId == clientId } ?: throw BusinessFinanceNotFoundException()
        if (current.document.version != expectedVersion) throw BusinessFinanceConflictException("The document was changed by another session")
        requireDraft(current.document.status)
        val updated = current.document.copy(status = BusinessSalesDocumentStatus.ARCHIVED, version = expectedVersion + 1)
        documents[documentId] = current.copy(document = updated)
        events += BusinessFinanceAuditEvent(clientId, "SALES_DOCUMENT", documentId, "ARCHIVED", actorUserId, updated.version)
        updated
    }

    override fun updateSalesDocument(
        clientId: String,
        actorUserId: String,
        documentId: String,
        expectedVersion: Int,
        draft: SalesDocumentDraftInputWithTotals,
    ): BusinessSalesDocument = synchronized(lock) {
        val current = documents[documentId]?.takeIf { it.clientId == clientId } ?: throw BusinessFinanceNotFoundException()
        if (current.document.version != expectedVersion) throw BusinessFinanceConflictException("The document was changed by another session")
        requireDraft(current.document.status)
        val updated = current.document.copy(
            kind = draft.input.kind,
            issuerName = draft.input.issuerName,
            customerName = draft.input.customerName,
            issueDate = draft.input.issueDate,
            dueDate = draft.input.dueDate,
            notes = draft.input.notes,
            currency = draft.input.currency,
            subtotalCents = draft.subtotalCents,
            taxCents = draft.taxCents,
            totalCents = draft.totalCents,
            lines = draft.lines,
            version = expectedVersion + 1,
        )
        documents[documentId] = current.copy(document = updated)
        events += BusinessFinanceAuditEvent(clientId, "SALES_DOCUMENT", documentId, "UPDATED", actorUserId, updated.version)
        updated
    }

    override fun listFinanceEntries(clientId: String): List<BusinessFinanceEntry> = synchronized(lock) {
        entries.values
            .asSequence()
            .filter { it.clientId == clientId }
            .map(StoredEntry::entry)
            .sortedWith(compareByDescending<BusinessFinanceEntry> { it.occurredOn }.thenByDescending { it.id })
            .toList()
    }

    override fun createFinanceEntry(
        clientId: String,
        actorUserId: String,
        entry: CalculatedFinanceEntryInput,
    ): BusinessFinanceEntry = synchronized(lock) {
        val created = BusinessFinanceEntry(
            id = idFactory(),
            direction = entry.input.direction,
            status = BusinessFinanceEntryStatus.DRAFT,
            occurredOn = entry.input.occurredOn,
            description = entry.input.description,
            netCents = entry.input.netCents,
            taxRateBasisPoints = entry.input.taxRateBasisPoints,
            taxCents = entry.taxCents,
            grossCents = entry.grossCents,
            counterpartyName = entry.input.counterpartyName,
            categoryName = entry.input.categoryName,
            paymentStatus = entry.input.paymentStatus,
            externalReference = entry.input.externalReference,
            currency = entry.input.currency,
            version = 1,
        )
        entries[created.id] = StoredEntry(clientId, created)
        events += BusinessFinanceAuditEvent(clientId, "FINANCE_ENTRY", created.id, "CREATED", actorUserId, created.version)
        created
    }

    override fun recordFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
    ): BusinessFinanceEntry = synchronized(lock) {
        val current = entries[entryId]?.takeIf { it.clientId == clientId } ?: throw BusinessFinanceNotFoundException()
        if (current.entry.version != expectedVersion) throw BusinessFinanceConflictException("The entry was changed by another session")
        requireDraftEntry(current.entry.status)
        val updated = current.entry.copy(status = BusinessFinanceEntryStatus.RECORDED, version = expectedVersion + 1)
        entries[entryId] = current.copy(entry = updated)
        events += BusinessFinanceAuditEvent(clientId, "FINANCE_ENTRY", entryId, "RECORDED", actorUserId, updated.version)
        updated
    }

    override fun updateFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
        entry: CalculatedFinanceEntryInput,
    ): BusinessFinanceEntry = synchronized(lock) {
        val current = entries[entryId]?.takeIf { it.clientId == clientId } ?: throw BusinessFinanceNotFoundException()
        if (current.entry.version != expectedVersion) throw BusinessFinanceConflictException("The entry was changed by another session")
        requireDraftEntry(current.entry.status)
        val updated = current.entry.copy(
            direction = entry.input.direction,
            occurredOn = entry.input.occurredOn,
            description = entry.input.description,
            netCents = entry.input.netCents,
            taxRateBasisPoints = entry.input.taxRateBasisPoints,
            taxCents = entry.taxCents,
            grossCents = entry.grossCents,
            counterpartyName = entry.input.counterpartyName,
            categoryName = entry.input.categoryName,
            paymentStatus = entry.input.paymentStatus,
            externalReference = entry.input.externalReference,
            currency = entry.input.currency,
            version = expectedVersion + 1,
        )
        entries[entryId] = current.copy(entry = updated)
        events += BusinessFinanceAuditEvent(clientId, "FINANCE_ENTRY", entryId, "UPDATED", actorUserId, updated.version)
        updated
    }

    override fun voidFinanceEntry(
        clientId: String,
        actorUserId: String,
        entryId: String,
        expectedVersion: Int,
        reason: String,
    ): BusinessFinanceEntry = synchronized(lock) {
        val current = entries[entryId]?.takeIf { it.clientId == clientId } ?: throw BusinessFinanceNotFoundException()
        if (current.entry.version != expectedVersion) throw BusinessFinanceConflictException("The entry was changed by another session")
        if (current.entry.status == BusinessFinanceEntryStatus.VOID) throw BusinessFinanceConflictException("The entry is already void")
        val updated = current.entry.copy(
            status = BusinessFinanceEntryStatus.VOID,
            voidReason = reason,
            version = expectedVersion + 1,
        )
        entries[entryId] = current.copy(entry = updated)
        events += BusinessFinanceAuditEvent(clientId, "FINANCE_ENTRY", entryId, "VOIDED", actorUserId, updated.version)
        updated
    }

    override fun financeOverview(clientId: String, period: String): FinanceOverview = synchronized(lock) {
        val periodEntries = entries.values
            .asSequence()
            .filter { it.clientId == clientId && it.entry.status != BusinessFinanceEntryStatus.VOID }
            .map(StoredEntry::entry)
            .filter { it.occurredOn.startsWith(period) }
            .toList()
        val income = periodEntries.filter { it.direction.name == "INCOME" }.sumOf { it.grossCents }
        val expenses = periodEntries.filter { it.direction.name == "EXPENSE" }.sumOf { it.grossCents }
        val pending = periodEntries.filter { it.paymentStatus == BusinessPaymentStatus.PENDING }.sumOf { it.grossCents }
        FinanceOverview(period, income, expenses, income - expenses, pending)
    }

    override fun auditEvents(clientId: String): List<BusinessFinanceAuditEvent> = synchronized(lock) {
        events.filter { it.clientId == clientId }.toList()
    }

    private data class StoredDocument(val clientId: String, val document: BusinessSalesDocument)
    private data class StoredEntry(val clientId: String, val entry: BusinessFinanceEntry)
}
