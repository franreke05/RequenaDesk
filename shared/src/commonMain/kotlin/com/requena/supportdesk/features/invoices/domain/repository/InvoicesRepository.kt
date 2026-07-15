package com.requena.supportdesk.features.invoices.domain.repository

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput

interface InvoicesRepository {
    fun buildGeneratedInvoiceUrl(input: CreateInvoiceInput): String
}
