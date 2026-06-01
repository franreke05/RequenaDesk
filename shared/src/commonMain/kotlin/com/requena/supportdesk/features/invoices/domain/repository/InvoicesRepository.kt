package com.requena.supportdesk.features.invoices.domain.repository

import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.Invoice
import com.requena.supportdesk.features.invoices.domain.model.InvoiceStatus

interface InvoicesRepository {
    suspend fun getInvoices(): AppResult<List<Invoice>>
    suspend fun getInvoice(id: String): AppResult<Invoice>
    suspend fun createInvoice(input: CreateInvoiceInput): AppResult<Invoice>
    suspend fun updateStatus(invoiceId: String, status: InvoiceStatus): AppResult<Invoice>
    suspend fun getPdfUrl(invoiceId: String): AppResult<String>
}
