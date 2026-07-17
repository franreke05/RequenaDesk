package com.requena.supportdesk.features.business.sales.domain

import kotlinx.serialization.Serializable

const val BUSINESS_CUSTOMERS = "BUSINESS_CUSTOMERS"
const val BUSINESS_QUOTES = "BUSINESS_QUOTES"
const val BUSINESS_CATALOG = "BUSINESS_CATALOG"

private const val EUR = "EUR"

@Serializable
enum class BusinessCustomerStatus { ACTIVE, ARCHIVED }

@Serializable
enum class BusinessCatalogItemType { PRODUCT, SERVICE }

@Serializable
enum class BusinessStockMovementType { INITIAL, ADJUSTMENT, SALE, RETURN }

@Serializable
enum class BusinessQuoteStatus { DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED }

@Serializable
enum class BusinessSaleStatus { CONFIRMED, CANCELLED }

@Serializable
data class BusinessCustomer(
    val id: String,
    val displayName: String,
    val taxId: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val status: BusinessCustomerStatus,
    val version: Int,
    val updatedAt: String,
)

@Serializable
data class BusinessCustomerDetail(
    val customer: BusinessCustomer,
    val contacts: List<BusinessContact>,
)

@Serializable
data class BusinessContact(
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
data class BusinessCatalogItem(
    val id: String,
    val type: BusinessCatalogItemType,
    val name: String,
    val sku: String? = null,
    val description: String? = null,
    val unit: String = "unidad",
    val referencePriceCents: Long,
    val currency: String = EUR,
    val tracksStock: Boolean = false,
    val stockMinimumMilli: Long? = null,
    val archived: Boolean = false,
    val version: Int,
    val updatedAt: String,
)

@Serializable
data class BusinessStockSummary(
    val item: BusinessCatalogItem,
    val availableMilli: Long,
    val isBelowMinimum: Boolean,
)

@Serializable
data class BusinessStockMovement(
    val id: String,
    val itemId: String,
    val type: BusinessStockMovementType,
    val deltaMilli: Long,
    val reason: String,
    val referenceId: String? = null,
    val createdAt: String,
)

@Serializable
data class BusinessQuoteLine(
    val id: String,
    val position: Int,
    val sourceCatalogItemId: String? = null,
    val description: String,
    /** Thousandths of one unit; avoids binary decimal quantities. */
    val quantityMilli: Long,
    val unitPriceCents: Long,
    val discountBasisPoints: Int,
    val taxBasisPoints: Int,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

@Serializable
data class BusinessQuote(
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
    val currency: String = EUR,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val version: Int,
    val lines: List<BusinessQuoteLine> = emptyList(),
    val updatedAt: String,
)

@Serializable
data class BusinessSale(
    val id: String,
    val number: String,
    val quoteId: String,
    val buyerName: String,
    val currency: String = EUR,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val status: BusinessSaleStatus,
    val confirmedAt: String,
    val lines: List<BusinessQuoteLine> = emptyList(),
)

@Serializable
data class BusinessSalesPage<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)

@Serializable
data class BusinessSalesPageRequest(
    val query: String? = null,
    val status: String? = null,
    val cursor: String? = null,
    val limit: Int = 40,
)

@Serializable
data class CreateBusinessCustomerInput(
    val displayName: String,
    val taxId: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
)

@Serializable
data class UpdateBusinessCustomerInput(
    val displayName: String,
    val taxId: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val expectedVersion: Int,
)

@Serializable
data class CreateBusinessContactInput(
    val fullName: String,
    val role: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val isPrimary: Boolean = false,
)

@Serializable
data class UpdateBusinessContactInput(
    val fullName: String,
    val role: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val isPrimary: Boolean = false,
    val status: BusinessCustomerStatus = BusinessCustomerStatus.ACTIVE,
    val expectedVersion: Int,
)

@Serializable
data class CreateBusinessCatalogItemInput(
    val type: BusinessCatalogItemType,
    val name: String,
    val sku: String? = null,
    val description: String? = null,
    val unit: String = "unidad",
    val referencePriceCents: Long,
    val tracksStock: Boolean = false,
    val stockMinimumMilli: Long? = null,
)

@Serializable
data class UpdateBusinessCatalogItemInput(
    val name: String,
    val sku: String? = null,
    val description: String? = null,
    val unit: String = "unidad",
    val referencePriceCents: Long,
    val tracksStock: Boolean = false,
    val stockMinimumMilli: Long? = null,
    val expectedVersion: Int,
)

@Serializable
data class StockAdjustmentInput(
    val type: BusinessStockMovementType = BusinessStockMovementType.ADJUSTMENT,
    val deltaMilli: Long,
    val reason: String,
    val idempotencyKey: String,
)

@Serializable
data class BusinessQuoteLineInput(
    val position: Int,
    val sourceCatalogItemId: String? = null,
    val description: String,
    val quantityMilli: Long,
    val unitPriceCents: Long,
    val discountBasisPoints: Int = 0,
    val taxBasisPoints: Int = 0,
)

@Serializable
data class CreateBusinessQuoteInput(
    val customerId: String? = null,
    val buyerName: String,
    val buyerEmail: String? = null,
    val buyerPhone: String? = null,
    val issueDate: String,
    val validUntil: String? = null,
    val notes: String? = null,
    val lines: List<BusinessQuoteLineInput>,
    val idempotencyKey: String,
)

@Serializable
data class UpdateBusinessQuoteInput(
    val customerId: String? = null,
    val buyerName: String,
    val buyerEmail: String? = null,
    val buyerPhone: String? = null,
    val issueDate: String,
    val validUntil: String? = null,
    val notes: String? = null,
    val lines: List<BusinessQuoteLineInput>,
    val expectedVersion: Int,
)

@Serializable
data class QuoteTransitionInput(
    val expectedVersion: Int,
)

@Serializable
data class ExpectedVersionInput(
    val expectedVersion: Int,
)

@Serializable
data class ConvertBusinessQuoteInput(
    val idempotencyKey: String,
)

class BusinessSalesValidationException(message: String) : IllegalArgumentException(message)

data class CalculatedBusinessQuoteInput(
    val source: CreateBusinessQuoteInput,
    val lines: List<BusinessQuoteLine>,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

data class CalculatedBusinessQuoteUpdate(
    val source: UpdateBusinessQuoteInput,
    val lines: List<BusinessQuoteLine>,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

/** Shared validation and money calculation. The server is the only authority that invokes it for writes. */
object BusinessSalesRules {
    private const val MAX_PRICE_CENTS = 1_000_000_000L
    private const val THOUSAND = 1_000L
    private const val BASIS_POINTS = 10_000L
    private val isoDate = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val idempotencyKey = Regex("[A-Za-z0-9._:-]{8,120}")

    fun validateCustomer(input: CreateBusinessCustomerInput) {
        required(input.displayName, "Customer name", 160)
        optional(input.taxId, "Tax identifier", 40)
        email(input.email)
        optional(input.phone, "Phone", 40)
        optional(input.address, "Address", 500)
    }

    fun validateCustomer(input: UpdateBusinessCustomerInput) {
        validateCustomer(CreateBusinessCustomerInput(input.displayName, input.taxId, input.email, input.phone, input.address))
        version(input.expectedVersion)
    }

    fun validateContact(input: CreateBusinessContactInput) {
        required(input.fullName, "Contact name", 160)
        optional(input.role, "Role", 120)
        email(input.email)
        optional(input.phone, "Phone", 40)
    }

    fun validateContact(input: UpdateBusinessContactInput) {
        validateContact(CreateBusinessContactInput(input.fullName, input.role, input.email, input.phone, input.isPrimary))
        version(input.expectedVersion)
    }

    fun validateCatalogItem(input: CreateBusinessCatalogItemInput) {
        required(input.name, "Item name", 160)
        optional(input.sku, "SKU", 80)
        optional(input.description, "Description", 1_000)
        required(input.unit, "Unit", 40)
        amount(input.referencePriceCents, "Reference price")
        if (input.type == BusinessCatalogItemType.SERVICE && input.tracksStock) invalid("Services cannot track stock")
        input.stockMinimumMilli?.let { if (it < 0) invalid("Stock minimum cannot be negative") }
        if (!input.tracksStock && input.stockMinimumMilli != null) invalid("Only stock-tracked products can have a stock minimum")
    }

    fun validateCatalogItem(input: UpdateBusinessCatalogItemInput, type: BusinessCatalogItemType) {
        validateCatalogItem(
            CreateBusinessCatalogItemInput(
                type = type,
                name = input.name,
                sku = input.sku,
                description = input.description,
                unit = input.unit,
                referencePriceCents = input.referencePriceCents,
                tracksStock = input.tracksStock,
                stockMinimumMilli = input.stockMinimumMilli,
            ),
        )
        version(input.expectedVersion)
    }

    fun validateStockAdjustment(input: StockAdjustmentInput) {
        if (input.type !in setOf(BusinessStockMovementType.INITIAL, BusinessStockMovementType.ADJUSTMENT, BusinessStockMovementType.RETURN)) {
            invalid("Only initial stock, adjustments and returns can be submitted")
        }
        if (input.deltaMilli == 0L) invalid("Stock delta cannot be zero")
        required(input.reason, "Adjustment reason", 240)
        validateIdempotencyKey(input.idempotencyKey)
    }

    fun calculateQuote(input: CreateBusinessQuoteInput): CalculatedBusinessQuoteInput {
        val normalized = input.normalized()
        validateQuote(normalized)
        val lines = normalized.lines.map(::calculateLine)
        return CalculatedBusinessQuoteInput(
            source = normalized,
            lines = lines,
            subtotalCents = lines.sumSafe { it.subtotalCents },
            taxCents = lines.sumSafe { it.taxCents },
            totalCents = lines.sumSafe { it.totalCents },
        )
    }

    fun calculateQuote(input: UpdateBusinessQuoteInput): CalculatedBusinessQuoteUpdate {
        val normalized = input.normalized()
        validateQuote(normalized)
        version(normalized.expectedVersion)
        val lines = normalized.lines.map(::calculateLine)
        return CalculatedBusinessQuoteUpdate(
            source = normalized,
            lines = lines,
            subtotalCents = lines.sumSafe { it.subtotalCents },
            taxCents = lines.sumSafe { it.taxCents },
            totalCents = lines.sumSafe { it.totalCents },
        )
    }

    fun validateTransition(input: QuoteTransitionInput) = version(input.expectedVersion)

    fun validateIdempotencyKey(value: String) {
        if (!idempotencyKey.matches(value.trim())) invalid("Idempotency key is invalid")
    }

    private fun CreateBusinessQuoteInput.normalized() = copy(
        buyerName = buyerName.trim(),
        buyerEmail = buyerEmail?.trim()?.takeIf(String::isNotBlank),
        buyerPhone = buyerPhone?.trim()?.takeIf(String::isNotBlank),
        notes = notes?.trim()?.takeIf(String::isNotBlank),
        idempotencyKey = idempotencyKey.trim(),
        lines = lines.map { it.copy(description = it.description.trim()) },
    )

    private fun UpdateBusinessQuoteInput.normalized() = copy(
        buyerName = buyerName.trim(),
        buyerEmail = buyerEmail?.trim()?.takeIf(String::isNotBlank),
        buyerPhone = buyerPhone?.trim()?.takeIf(String::isNotBlank),
        notes = notes?.trim()?.takeIf(String::isNotBlank),
        lines = lines.map { it.copy(description = it.description.trim()) },
    )

    private fun validateQuote(input: CreateBusinessQuoteInput) {
        required(input.buyerName, "Buyer name", 160)
        email(input.buyerEmail)
        optional(input.buyerPhone, "Buyer phone", 40)
        optional(input.notes, "Notes", 2_000)
        date(input.issueDate, "Issue date")
        input.validUntil?.let {
            date(it, "Valid-until date")
            if (it < input.issueDate) invalid("Valid-until date cannot precede issue date")
        }
        if (input.lines.size !in 1..100) invalid("A quote needs between 1 and 100 lines")
        if (input.lines.map { it.position }.toSet().size != input.lines.size || input.lines.any { it.position < 1 }) {
            invalid("Quote line positions must be unique and positive")
        }
        validateIdempotencyKey(input.idempotencyKey)
    }

    private fun validateQuote(input: UpdateBusinessQuoteInput) {
        required(input.buyerName, "Buyer name", 160)
        email(input.buyerEmail)
        optional(input.buyerPhone, "Buyer phone", 40)
        optional(input.notes, "Notes", 2_000)
        date(input.issueDate, "Issue date")
        input.validUntil?.let {
            date(it, "Valid-until date")
            if (it < input.issueDate) invalid("Valid-until date cannot precede issue date")
        }
        if (input.lines.size !in 1..100) invalid("A quote needs between 1 and 100 lines")
        if (input.lines.map { it.position }.toSet().size != input.lines.size || input.lines.any { it.position < 1 }) {
            invalid("Quote line positions must be unique and positive")
        }
    }

    private fun calculateLine(input: BusinessQuoteLineInput): BusinessQuoteLine {
        required(input.description, "Line description", 1_000)
        if (input.quantityMilli <= 0) invalid("Line quantity must be positive")
        amount(input.unitPriceCents, "Unit price")
        basisPoints(input.discountBasisPoints, "Discount")
        basisPoints(input.taxBasisPoints, "Tax")
        val beforeDiscount = multiplyDivide(input.quantityMilli, input.unitPriceCents, THOUSAND)
        val discount = multiplyDivide(beforeDiscount, input.discountBasisPoints.toLong(), BASIS_POINTS)
        val subtotal = beforeDiscount - discount
        val tax = multiplyDivide(subtotal, input.taxBasisPoints.toLong(), BASIS_POINTS)
        return BusinessQuoteLine(
            id = "",
            position = input.position,
            sourceCatalogItemId = input.sourceCatalogItemId,
            description = input.description.trim(),
            quantityMilli = input.quantityMilli,
            unitPriceCents = input.unitPriceCents,
            discountBasisPoints = input.discountBasisPoints,
            taxBasisPoints = input.taxBasisPoints,
            subtotalCents = subtotal,
            taxCents = tax,
            totalCents = safeAdd(subtotal, tax),
        )
    }

    private fun required(value: String, label: String, max: Int) {
        if (value.trim().length !in 1..max) invalid("$label is invalid")
    }

    private fun optional(value: String?, label: String, max: Int) {
        if (value != null && value.trim().length > max) invalid("$label is invalid")
    }

    private fun email(value: String?) {
        if (value != null && value.isNotBlank() && (value.length > 254 || !value.contains('@'))) invalid("Email is invalid")
    }

    private fun date(value: String, label: String) {
        if (!isoDate.matches(value)) invalid("$label must use YYYY-MM-DD")
    }

    private fun amount(value: Long, label: String) {
        if (value !in 0..MAX_PRICE_CENTS) invalid("$label is invalid")
    }

    private fun basisPoints(value: Int, label: String) {
        if (value !in 0..BASIS_POINTS.toInt()) invalid("$label is invalid")
    }

    private fun version(value: Int) {
        if (value < 1) invalid("Expected version is invalid")
    }

    private fun multiplyDivide(value: Long, multiplier: Long, divisor: Long): Long {
        if (value == 0L || multiplier == 0L) return 0L
        if (value > Long.MAX_VALUE / multiplier) invalid("Amount is too large")
        val product = value * multiplier
        val half = divisor / 2
        if (product > Long.MAX_VALUE - half) invalid("Amount is too large")
        return (product + half) / divisor
    }

    private fun safeAdd(left: Long, right: Long): Long {
        if (right > Long.MAX_VALUE - left) invalid("Amount is too large")
        return left + right
    }

    private fun Iterable<BusinessQuoteLine>.sumSafe(selector: (BusinessQuoteLine) -> Long): Long =
        fold(0L) { total, line -> safeAdd(total, selector(line)) }

    private fun invalid(message: String): Nothing = throw BusinessSalesValidationException(message)
}
