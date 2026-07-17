package com.requena.supportdesk.features.invoices.data.storage

import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput

actual fun createInvoicePdfStorage(): InvoicePdfStorage = UnsupportedInvoicePdfStorage

private object UnsupportedInvoicePdfStorage : InvoicePdfStorage {
    override suspend fun listSavedInvoices(): List<InvoicePdfFile> = emptyList()

    override suspend fun saveInvoice(input: CreateInvoiceInput): InvoicePdfFile =
        error("La creacion local de facturas esta disponible en la aplicacion de escritorio.")

    override suspend fun openSavedInvoice(fileName: String) {
        error("La biblioteca de facturas locales esta disponible en la aplicacion de escritorio.")
    }

    override suspend fun deleteSavedInvoice(fileName: String) {
        error("La biblioteca de facturas locales esta disponible en la aplicacion de escritorio.")
    }
}
