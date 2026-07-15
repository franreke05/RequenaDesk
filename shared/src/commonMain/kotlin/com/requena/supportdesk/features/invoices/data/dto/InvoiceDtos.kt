package com.requena.supportdesk.features.invoices.data.dto

import kotlinx.serialization.Serializable

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
