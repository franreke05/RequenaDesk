package com.requena.supportdesk.features.business.sales.data.dto

import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItemType
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerType
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerStage
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerActivityType
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessSaleStatus
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovementType
import kotlinx.serialization.Serializable

@Serializable
data class BusinessCustomerDto(
    val id: String,
    val displayName: String,
    val taxId: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val status: BusinessCustomerStatus,
    val version: Int,
    val updatedAt: String,
    val type: BusinessCustomerType = BusinessCustomerType.COMPANY,
    val stage: BusinessCustomerStage = BusinessCustomerStage.CUSTOMER,
    val source: String? = null,
    val tags: List<BusinessCustomerTagDto> = emptyList(),
    val nextAction: BusinessCustomerActivityDto? = null,
)

@Serializable
data class BusinessContactDto(
    val id: String,
    val customerId: String,
    val fullName: String,
    val role: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val isPrimary: Boolean,
    val status: BusinessCustomerStatus,
    val version: Int,
    val updatedAt: String,
)

@Serializable
data class BusinessCustomerDetailDto(
    val customer: BusinessCustomerDto,
    val contacts: List<BusinessContactDto>,
    val tags: List<BusinessCustomerTagDto> = emptyList(),
    val activities: List<BusinessCustomerActivityDto> = emptyList(),
    val nextAction: BusinessCustomerActivityDto? = null,
)

@Serializable
data class BusinessCustomerTagDto(
    val id: String,
    val name: String,
    val colorHex: String,
    val archived: Boolean,
    val version: Int,
    val updatedAt: String,
)

@Serializable
data class BusinessCustomerActivityDto(
    val id: String,
    val customerId: String,
    val contactId: String? = null,
    val contactName: String? = null,
    val type: BusinessCustomerActivityType,
    val subject: String,
    val details: String? = null,
    val occurredAt: String? = null,
    val dueAt: String? = null,
    val completedAt: String? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BusinessCatalogItemDto(
    val id: String,
    val type: BusinessCatalogItemType,
    val name: String,
    val sku: String? = null,
    val description: String? = null,
    val unit: String,
    val referencePriceCents: Long,
    val currency: String,
    val tracksStock: Boolean,
    val stockMinimumMilli: Long? = null,
    val archived: Boolean,
    val version: Int,
    val updatedAt: String,
)

@Serializable
data class BusinessStockSummaryDto(
    val item: BusinessCatalogItemDto,
    val availableMilli: Long,
    val isBelowMinimum: Boolean,
)

@Serializable
data class BusinessStockMovementDto(
    val id: String,
    val itemId: String,
    val type: BusinessStockMovementType,
    val deltaMilli: Long,
    val reason: String,
    val referenceId: String? = null,
    val createdAt: String,
)

@Serializable
data class BusinessQuoteLineDto(
    val id: String,
    val position: Int,
    val sourceCatalogItemId: String? = null,
    val description: String,
    val quantityMilli: Long,
    val unitPriceCents: Long,
    val discountBasisPoints: Int,
    val taxBasisPoints: Int,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

@Serializable
data class BusinessQuoteDto(
    val id: String,
    val number: String,
    val customerId: String? = null,
    val buyerName: String,
    val buyerEmail: String? = null,
    val buyerPhone: String? = null,
    val status: BusinessQuoteStatus,
    val issueDate: String,
    val validUntil: String? = null,
    val notes: String? = null,
    val currency: String,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val version: Int,
    val lines: List<BusinessQuoteLineDto> = emptyList(),
    val updatedAt: String,
)

@Serializable
data class BusinessSaleDto(
    val id: String,
    val number: String,
    val quoteId: String,
    val buyerName: String,
    val currency: String,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val status: BusinessSaleStatus,
    val confirmedAt: String,
    val lines: List<BusinessQuoteLineDto> = emptyList(),
)

@Serializable
data class BusinessSalesPageDto<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)
