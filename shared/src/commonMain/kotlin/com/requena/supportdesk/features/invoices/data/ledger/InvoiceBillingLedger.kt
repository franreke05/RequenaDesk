package com.requena.supportdesk.features.invoices.data.ledger

/**
 * Local, append-only record of which time-tracking units (TaskLog ids) have been billed under
 * which invoice number. Per CLAUDE.md, invoices themselves are never persisted anywhere - this
 * ledger does not store invoice line items, prices, or metadata, only the minimal association
 * needed to stop the same time from being billed twice and to let a void release it again. It
 * lives entirely on the local filesystem next to the PDFs (no database, no Ktor, no server).
 *
 * Known limitation (explicitly accepted): this ledger is local to the machine generating
 * invoices. It does not synchronize across devices or multiple installs.
 */
interface InvoiceBillingLedger {
    /** Every TaskLog id currently billed under a non-voided invoice. */
    suspend fun billedLogIds(): Set<String>

    /**
     * Atomically assigns the next correlative number of the [issuedAt] year series
     * (FAC-2026-0001, FAC-2026-0002...) and records it as issued with [logIds]. Spanish invoicing
     * rules require consecutive numbers in date order, so this throws
     * [InvoiceNumberingException] when [issuedAt] is earlier than the last invoice of the series,
     * and [DuplicateBillingException] when any id is already billed under a non-voided invoice.
     */
    suspend fun issueInvoice(issuedAt: String, logIds: List<String>): String

    /** Releases every id billed under [invoiceNumber] back to pending. Returns the released ids. */
    suspend fun recordVoided(invoiceNumber: String, reason: String?): List<String>

    /** True once [invoiceNumber] has an issued entry that has not been voided. */
    suspend fun isIssuedAndNotVoided(invoiceNumber: String): Boolean

    /** Every invoice number that has a VOIDED entry - used to render "Anulada" in the library list. */
    suspend fun voidedInvoiceNumbers(): Set<String>
}

class DuplicateBillingException(message: String) : IllegalStateException(message)

class InvoiceNumberingException(message: String) : IllegalStateException(message)

expect fun createInvoiceBillingLedger(): InvoiceBillingLedger
