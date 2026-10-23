package com.requena.supportdesk.features.invoices.data.ledger

actual fun createInvoiceBillingLedger(): InvoiceBillingLedger = UnsupportedInvoiceBillingLedger

private object UnsupportedInvoiceBillingLedger : InvoiceBillingLedger {
    override suspend fun billedLogIds(): Set<String> = emptySet()

    override suspend fun issueInvoice(issuedAt: String, logIds: List<String>): String =
        error("La facturacion local esta disponible en la aplicacion de escritorio.")

    override suspend fun recordVoided(invoiceNumber: String, reason: String?): List<String> = emptyList()

    override suspend fun isIssuedAndNotVoided(invoiceNumber: String): Boolean = false

    override suspend fun voidedInvoiceNumbers(): Set<String> = emptySet()
}
