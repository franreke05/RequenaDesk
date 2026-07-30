package com.requena.supportdesk.features.invoices.data.ledger

actual fun createInvoiceBillingLedger(): InvoiceBillingLedger = UnsupportedInvoiceBillingLedger

private object UnsupportedInvoiceBillingLedger : InvoiceBillingLedger {
    override fun nextInvoiceNumber(issuedAt: String): String =
        error("La facturacion local esta disponible en la aplicacion de escritorio.")

    override suspend fun billedLogIds(): Set<String> = emptySet()

    override suspend fun recordIssued(invoiceNumber: String, logIds: List<String>) {
        error("La facturacion local esta disponible en la aplicacion de escritorio.")
    }

    override suspend fun recordVoided(invoiceNumber: String, reason: String?): List<String> = emptyList()

    override suspend fun isIssuedAndNotVoided(invoiceNumber: String): Boolean = false

    override suspend fun voidedInvoiceNumbers(): Set<String> = emptySet()
}
