package com.requena.supportdesk.features.invoices.domain.usecase

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.repository.InvoicesRepository

class GenerateInvoiceUseCase(private val repository: InvoicesRepository) {
    operator fun invoke(input: CreateInvoiceInput): String = repository.buildGeneratedInvoiceUrl(input)
}
