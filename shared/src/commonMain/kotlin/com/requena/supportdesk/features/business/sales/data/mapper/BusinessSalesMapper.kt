package com.requena.supportdesk.features.business.sales.data.mapper

import com.requena.supportdesk.features.business.sales.data.dto.BusinessCatalogItemDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessContactDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessCustomerDetailDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessCustomerDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessQuoteDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessQuoteLineDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessSaleDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessSalesPageDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessStockMovementDto
import com.requena.supportdesk.features.business.sales.data.dto.BusinessStockSummaryDto
import com.requena.supportdesk.features.business.sales.domain.BusinessCatalogItem
import com.requena.supportdesk.features.business.sales.domain.BusinessContact
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomer
import com.requena.supportdesk.features.business.sales.domain.BusinessCustomerDetail
import com.requena.supportdesk.features.business.sales.domain.BusinessQuote
import com.requena.supportdesk.features.business.sales.domain.BusinessQuoteLine
import com.requena.supportdesk.features.business.sales.domain.BusinessSale
import com.requena.supportdesk.features.business.sales.domain.BusinessSalesPage
import com.requena.supportdesk.features.business.sales.domain.BusinessStockMovement
import com.requena.supportdesk.features.business.sales.domain.BusinessStockSummary

object BusinessSalesMapper {
    fun customer(dto: BusinessCustomerDto) = BusinessCustomer(
        id = dto.id,
        displayName = dto.displayName,
        taxId = dto.taxId,
        email = dto.email,
        phone = dto.phone,
        address = dto.address,
        status = dto.status,
        version = dto.version,
        updatedAt = dto.updatedAt,
    )

    fun customerDto(model: BusinessCustomer) = BusinessCustomerDto(
        model.id, model.displayName, model.taxId, model.email, model.phone, model.address, model.status, model.version, model.updatedAt,
    )

    fun contact(dto: BusinessContactDto) = BusinessContact(
        dto.id, dto.customerId, dto.fullName, dto.role, dto.email, dto.phone, dto.isPrimary, dto.status, dto.version, dto.updatedAt,
    )

    fun contactDto(model: BusinessContact) = BusinessContactDto(
        model.id, model.customerId, model.fullName, model.role, model.email, model.phone, model.isPrimary, model.status, model.version, model.updatedAt,
    )

    fun customerDetail(dto: BusinessCustomerDetailDto) = BusinessCustomerDetail(customer(dto.customer), dto.contacts.map(::contact))
    fun customerDetailDto(model: BusinessCustomerDetail) = BusinessCustomerDetailDto(customerDto(model.customer), model.contacts.map(::contactDto))

    fun catalogItem(dto: BusinessCatalogItemDto) = BusinessCatalogItem(
        dto.id, dto.type, dto.name, dto.sku, dto.description, dto.unit, dto.referencePriceCents, dto.currency,
        dto.tracksStock, dto.stockMinimumMilli, dto.archived, dto.version, dto.updatedAt,
    )

    fun catalogItemDto(model: BusinessCatalogItem) = BusinessCatalogItemDto(
        model.id, model.type, model.name, model.sku, model.description, model.unit, model.referencePriceCents, model.currency,
        model.tracksStock, model.stockMinimumMilli, model.archived, model.version, model.updatedAt,
    )

    fun stockSummary(dto: BusinessStockSummaryDto) = BusinessStockSummary(catalogItem(dto.item), dto.availableMilli, dto.isBelowMinimum)
    fun stockSummaryDto(model: BusinessStockSummary) = BusinessStockSummaryDto(catalogItemDto(model.item), model.availableMilli, model.isBelowMinimum)

    fun stockMovement(dto: BusinessStockMovementDto) = BusinessStockMovement(dto.id, dto.itemId, dto.type, dto.deltaMilli, dto.reason, dto.referenceId, dto.createdAt)
    fun stockMovementDto(model: BusinessStockMovement) = BusinessStockMovementDto(model.id, model.itemId, model.type, model.deltaMilli, model.reason, model.referenceId, model.createdAt)

    fun quoteLine(dto: BusinessQuoteLineDto) = BusinessQuoteLine(
        dto.id, dto.position, dto.sourceCatalogItemId, dto.description, dto.quantityMilli, dto.unitPriceCents,
        dto.discountBasisPoints, dto.taxBasisPoints, dto.subtotalCents, dto.taxCents, dto.totalCents,
    )

    fun quoteLineDto(model: BusinessQuoteLine) = BusinessQuoteLineDto(
        model.id, model.position, model.sourceCatalogItemId, model.description, model.quantityMilli, model.unitPriceCents,
        model.discountBasisPoints, model.taxBasisPoints, model.subtotalCents, model.taxCents, model.totalCents,
    )

    fun quote(dto: BusinessQuoteDto) = BusinessQuote(
        dto.id, dto.number, dto.customerId, dto.buyerName, dto.buyerEmail, dto.buyerPhone, dto.status, dto.issueDate,
        dto.validUntil, dto.notes, dto.currency, dto.subtotalCents, dto.taxCents, dto.totalCents, dto.version,
        dto.lines.map(::quoteLine), dto.updatedAt,
    )

    fun quoteDto(model: BusinessQuote) = BusinessQuoteDto(
        model.id, model.number, model.customerId, model.buyerName, model.buyerEmail, model.buyerPhone, model.status,
        model.issueDate, model.validUntil, model.notes, model.currency, model.subtotalCents, model.taxCents,
        model.totalCents, model.version, model.lines.map(::quoteLineDto), model.updatedAt,
    )

    fun sale(dto: BusinessSaleDto) = BusinessSale(
        dto.id, dto.number, dto.quoteId, dto.buyerName, dto.currency, dto.subtotalCents, dto.taxCents, dto.totalCents,
        dto.status, dto.confirmedAt, dto.lines.map(::quoteLine),
    )

    fun saleDto(model: BusinessSale) = BusinessSaleDto(
        model.id, model.number, model.quoteId, model.buyerName, model.currency, model.subtotalCents, model.taxCents,
        model.totalCents, model.status, model.confirmedAt, model.lines.map(::quoteLineDto),
    )

    fun <T, R> page(dto: BusinessSalesPageDto<T>, mapper: (T) -> R) = BusinessSalesPage(dto.items.map(mapper), dto.nextCursor)
    fun <T, R> pageDto(model: BusinessSalesPage<T>, mapper: (T) -> R) = BusinessSalesPageDto(model.items.map(mapper), model.nextCursor)
}
