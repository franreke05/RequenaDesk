package com.requena.supportdesk.features.invoices.data.profile

import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.InvoiceRecipient

actual fun createInvoiceProfileStore(): InvoiceProfileStore = UnsupportedInvoiceProfileStore

/** Invoices are only generated in the desktop app; mobile keeps nothing. */
private object UnsupportedInvoiceProfileStore : InvoiceProfileStore {
    override suspend fun loadIssuer(): InvoiceIssuer = InvoiceIssuer()
    override suspend fun saveIssuer(issuer: InvoiceIssuer) = Unit
    override suspend fun loadRecipient(clientId: String): InvoiceRecipient? = null
    override suspend fun saveRecipient(clientId: String, recipient: InvoiceRecipient) = Unit
}
