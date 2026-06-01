package com.requena.supportdesk.features.invoices.domain.usecase

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceStatus
import com.requena.supportdesk.features.invoices.domain.repository.InvoicesRepository

class GetInvoicesUseCase(private val repository: InvoicesRepository) {
    suspend operator fun invoke() = repository.getInvoices()
}

class GetInvoiceUseCase(private val repository: InvoicesRepository) {
    suspend operator fun invoke(id: String) = repository.getInvoice(id)
}

class CreateInvoiceUseCase(private val repository: InvoicesRepository) {
    suspend operator fun invoke(input: CreateInvoiceInput) = repository.createInvoice(input)
}

class UpdateInvoiceStatusUseCase(private val repository: InvoicesRepository) {
    suspend operator fun invoke(invoiceId: String, status: InvoiceStatus) =
        repository.updateStatus(invoiceId, status)
}

class GetInvoicePdfUrlUseCase(private val repository: InvoicesRepository) {
    suspend operator fun invoke(invoiceId: String) = repository.getPdfUrl(invoiceId)
}
