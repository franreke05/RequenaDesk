package com.requena.supportdesk.features.invoices.data.storage

import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput

/**
 * Platform boundary for the local invoice library. Invoice creation must not
 * perform network requests or persist invoice data in a database.
 */
interface InvoicePdfStorage {
    suspend fun listSavedInvoices(): List<InvoicePdfFile>
    suspend fun saveInvoice(input: CreateInvoiceInput): InvoicePdfFile
    suspend fun openSavedInvoice(fileName: String)
    suspend fun deleteSavedInvoice(fileName: String)
}

expect fun createInvoicePdfStorage(): InvoicePdfStorage
