package com.requena.supportdesk.features.invoices.data.storage

import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput

/**
 * Platform boundary for the local invoice library. Invoice creation must not
 * perform network requests or persist invoice data in a database.
 */
interface InvoicePdfStorage {
    suspend fun listSavedInvoices(): List<InvoicePdfFile>

    /** [invoiceNumber] is assigned up front by InvoiceBillingLedger.issueInvoice. */
    suspend fun saveInvoice(input: CreateInvoiceInput, invoiceNumber: String): InvoicePdfFile
    suspend fun openSavedInvoice(fileName: String)

    // No delete on purpose: an issued invoice must be kept. Mistakes are voided (and later
    // corrected with a rectifying invoice), never erased.
}

expect fun createInvoicePdfStorage(): InvoicePdfStorage
