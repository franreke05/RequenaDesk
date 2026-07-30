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
    /** Ephemeral, never written anywhere but into the PDF itself and this ledger's issued entries. */
    fun nextInvoiceNumber(issuedAt: String): String

    /** Every TaskLog id currently billed under a non-voided invoice. */
    suspend fun billedLogIds(): Set<String>

    /**
     * Marks [logIds] as billed under [invoiceNumber]. Throws if any id is already billed under
     * another non-voided invoice - the single enforcement point that a real unique constraint
     * would provide in a database-backed design.
     */
    suspend fun recordIssued(invoiceNumber: String, logIds: List<String>)

    /** Releases every id billed under [invoiceNumber] back to pending. Returns the released ids. */
    suspend fun recordVoided(invoiceNumber: String, reason: String?): List<String>

    /** True once [invoiceNumber] has an issued entry that has not been voided. */
    suspend fun isIssuedAndNotVoided(invoiceNumber: String): Boolean

    /** Every invoice number that has a VOIDED entry - used to render "Anulada" in the library list. */
    suspend fun voidedInvoiceNumbers(): Set<String>
}

class DuplicateBillingException(message: String) : IllegalStateException(message)

expect fun createInvoiceBillingLedger(): InvoiceBillingLedger
