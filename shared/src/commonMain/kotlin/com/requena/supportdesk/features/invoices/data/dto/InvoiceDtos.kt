package com.requena.supportdesk.features.invoices.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceItemDto(
    val id: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val sortOrder: Int = 0,
)

@Serializable
data class InvoiceDto(
    val id: String,
    val invoiceNumber: String,
    val clientId: String,
    val clientName: String = "",
    val status: String,
    val issuedAt: String,
    val dueAt: String? = null,
    val notes: String? = null,
    val taxPercent: Double = 0.0,
    val items: List<InvoiceItemDto> = emptyList(),
    val createdAt: String = "",
    val sentAt: String? = null,
    val paidAt: String? = null,
)

@Serializable
data class CreateInvoiceItemRequestDto(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val sortOrder: Int,
)

@Serializable
data class CreateInvoiceRequestDto(
    val clientId: String,
    val issuedAt: String,
    val dueAt: String?,
    val notes: String?,
    val taxPercent: Double,
    val items: List<CreateInvoiceItemRequestDto>,
)

@Serializable
data class UpdateInvoiceStatusRequestDto(
    val status: String,
)

@Serializable
data class InvoicePdfUrlDto(
    val url: String,
)
