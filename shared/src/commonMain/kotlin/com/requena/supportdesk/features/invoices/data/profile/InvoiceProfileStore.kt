package com.requena.supportdesk.features.invoices.data.profile

import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.InvoiceRecipient

/**
 * Local-only store for the fiscal data printed on invoices: the issuer (the self-employed
 * user) and the last fiscal data used for each client. It never touches the database or the
 * server (see CLAUDE.md); it only saves typing between invoices on this machine.
 */
interface InvoiceProfileStore {
    suspend fun loadIssuer(): InvoiceIssuer
    suspend fun saveIssuer(issuer: InvoiceIssuer)
    suspend fun loadRecipient(clientId: String): InvoiceRecipient?
    suspend fun saveRecipient(clientId: String, recipient: InvoiceRecipient)
}

expect fun createInvoiceProfileStore(): InvoiceProfileStore
