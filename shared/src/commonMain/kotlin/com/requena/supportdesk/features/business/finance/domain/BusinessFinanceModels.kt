package com.requena.supportdesk.features.business.finance.domain

import kotlinx.serialization.Serializable

const val BUSINESS_INVOICING = "BUSINESS_INVOICING"
const val BUSINESS_ACCOUNTING = "BUSINESS_ACCOUNTING"
const val FINANCE_BETA_DISCLAIMER = "BORRADOR DE PRUEBA — NO VÁLIDO COMO FACTURA FISCAL"

@Serializable
enum class BusinessSalesDocumentKind { DRAFT_INVOICE, PROFORMA }

@Serializable
enum class BusinessSalesDocumentStatus { DRAFT, ARCHIVED, VOID }

@Serializable
enum class BusinessFinanceDirection { INCOME, EXPENSE }

@Serializable
enum class BusinessFinanceEntryStatus { DRAFT, RECORDED, VOID }

@Serializable
enum class BusinessPaymentStatus { PENDING, PAID }

@Serializable
data class SalesLineInput(
    val description: String,
    /** Thousandths of one unit. This avoids binary decimal quantities. */
    val quantityMilli: Long,
    val unitPriceCents: Long,
    val taxRateBasisPoints: Int = 0,
    val discountBasisPoints: Int = 0,
)

@Serializable
data class CalculatedSalesLine(
    val description: String,
    val quantityMilli: Long,
    val unitPriceCents: Long,
    val taxRateBasisPoints: Int,
    val discountBasisPoints: Int,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

@Serializable
data class SalesDocumentDraftInput(
    val kind: BusinessSalesDocumentKind = BusinessSalesDocumentKind.DRAFT_INVOICE,
    val issuerName: String,
    val customerName: String,
    val issueDate: String,
    val dueDate: String? = null,
    val notes: String? = null,
    val currency: String = "EUR",
    val lines: List<SalesLineInput>,
)

@Serializable
data class BusinessSalesDocument(
    val id: String,
    val kind: BusinessSalesDocumentKind,
    val status: BusinessSalesDocumentStatus,
    val issuerName: String,
    val customerName: String,
    val issueDate: String,
    val dueDate: String? = null,
    val notes: String? = null,
    val currency: String = "EUR",
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val version: Int,
    val lines: List<CalculatedSalesLine>,
    val betaDisclaimer: String = FINANCE_BETA_DISCLAIMER,
)

@Serializable
data class FinanceEntryInput(
    val direction: BusinessFinanceDirection,
    val occurredOn: String,
    val description: String,
    val netCents: Long,
    val taxRateBasisPoints: Int = 0,
    val counterpartyName: String? = null,
    val categoryName: String? = null,
    val paymentStatus: BusinessPaymentStatus = BusinessPaymentStatus.PENDING,
    val externalReference: String? = null,
    val currency: String = "EUR",
)

@Serializable
data class BusinessFinanceEntry(
    val id: String,
    val direction: BusinessFinanceDirection,
    val status: BusinessFinanceEntryStatus,
    val occurredOn: String,
    val description: String,
    val netCents: Long,
    val taxRateBasisPoints: Int,
    val taxCents: Long,
    val grossCents: Long,
    val counterpartyName: String? = null,
    val categoryName: String? = null,
    val paymentStatus: BusinessPaymentStatus,
    val externalReference: String? = null,
    val currency: String = "EUR",
    val voidReason: String? = null,
    val version: Int,
)

@Serializable
data class FinanceOverview(
    val period: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val netCashFlowCents: Long,
    val pendingCents: Long,
    val currency: String = "EUR",
    val isInformationalOnly: Boolean = true,
)

class BusinessFinanceValidationException(message: String) : IllegalArgumentException(message)

object BusinessFinanceCalculator {
    private const val QUANTITY_SCALE = 1_000L
    private const val BASIS_POINT_SCALE = 10_000L
    private const val MAX_TEXT_LENGTH = 1_000

    fun calculateSalesDocument(input: SalesDocumentDraftInput): SalesDocumentDraftInputWithTotals {
        validateText(input.issuerName, "Issuer name")
        validateText(input.customerName, "Customer name")
        validateDate(input.issueDate, "Issue date")
        input.dueDate?.let {
            validateDate(it, "Due date")
            if (it < input.issueDate) invalid("Due date cannot precede issue date")
        }
        if (input.currency != "EUR") invalid("Only EUR is available in beta")
        if (input.lines.isEmpty() || input.lines.size > 100) invalid("A document must contain between 1 and 100 lines")
        val lines = input.lines.map(::calculateSalesLine)
        val subtotal = lines.fold(0L) { total, line -> safeAdd(total, line.subtotalCents) }
        val tax = lines.fold(0L) { total, line -> safeAdd(total, line.taxCents) }
        return SalesDocumentDraftInputWithTotals(
            input = input.copy(
                issuerName = input.issuerName.trim(),
                customerName = input.customerName.trim(),
                notes = input.notes?.trim()?.takeIf(String::isNotBlank),
            ),
            lines = lines,
            subtotalCents = subtotal,
            taxCents = tax,
            totalCents = safeAdd(subtotal, tax),
        )
    }

    fun calculateSalesLine(input: SalesLineInput): CalculatedSalesLine {
        validateText(input.description, "Line description")
        if (input.quantityMilli <= 0) invalid("Quantity must be greater than zero")
        if (input.unitPriceCents < 0) invalid("Unit price cannot be negative")
        validateBasisPoints(input.taxRateBasisPoints, "Tax rate")
        validateBasisPoints(input.discountBasisPoints, "Discount")
        val beforeDiscount = multiplyDivideRoundHalfUp(input.quantityMilli, input.unitPriceCents, QUANTITY_SCALE)
        val discount = multiplyDivideRoundHalfUp(beforeDiscount, input.discountBasisPoints.toLong(), BASIS_POINT_SCALE)
        val subtotal = beforeDiscount - discount
        val tax = multiplyDivideRoundHalfUp(subtotal, input.taxRateBasisPoints.toLong(), BASIS_POINT_SCALE)
        return CalculatedSalesLine(
            description = input.description.trim(),
            quantityMilli = input.quantityMilli,
            unitPriceCents = input.unitPriceCents,
            taxRateBasisPoints = input.taxRateBasisPoints,
            discountBasisPoints = input.discountBasisPoints,
            subtotalCents = subtotal,
            taxCents = tax,
            totalCents = safeAdd(subtotal, tax),
        )
    }

    fun calculateFinanceEntry(input: FinanceEntryInput): CalculatedFinanceEntryInput {
        validateDate(input.occurredOn, "Occurrence date")
        validateText(input.description, "Description")
        if (input.netCents < 0) invalid("Net amount cannot be negative")
        validateBasisPoints(input.taxRateBasisPoints, "Tax rate")
        if (input.currency != "EUR") invalid("Only EUR is available in beta")
        input.counterpartyName?.let { validateOptionalText(it, "Counterparty") }
        input.categoryName?.let { validateOptionalText(it, "Category") }
        input.externalReference?.let { validateOptionalText(it, "External reference") }
        val tax = multiplyDivideRoundHalfUp(input.netCents, input.taxRateBasisPoints.toLong(), BASIS_POINT_SCALE)
        return CalculatedFinanceEntryInput(
            input = input.copy(
                description = input.description.trim(),
                counterpartyName = input.counterpartyName?.trim()?.takeIf(String::isNotBlank),
                categoryName = input.categoryName?.trim()?.takeIf(String::isNotBlank),
                externalReference = input.externalReference?.trim()?.takeIf(String::isNotBlank),
            ),
            taxCents = tax,
            grossCents = safeAdd(input.netCents, tax),
        )
    }

    private fun validateDate(value: String, label: String) {
        val parts = value.split('-')
        if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) {
            invalid("$label must use YYYY-MM-DD")
        }
        val year = parts[0].toIntOrNull() ?: invalid("$label must use YYYY-MM-DD")
        val month = parts[1].toIntOrNull() ?: invalid("$label must use YYYY-MM-DD")
        val day = parts[2].toIntOrNull() ?: invalid("$label must use YYYY-MM-DD")
        if (year !in 1900..9999 || month !in 1..12 || day !in 1..daysInMonth(year, month)) {
            invalid("$label is not a real calendar date")
        }
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
    }

    private fun validateText(value: String, label: String) {
        if (value.trim().isEmpty() || value.trim().length > MAX_TEXT_LENGTH) invalid("$label is invalid")
    }

    private fun validateOptionalText(value: String, label: String) {
        if (value.trim().length > MAX_TEXT_LENGTH) invalid("$label is invalid")
    }

    private fun validateBasisPoints(value: Int, label: String) {
        if (value !in 0..BASIS_POINT_SCALE.toInt()) invalid("$label must be between 0 and 10000 basis points")
    }

    private fun multiplyDivideRoundHalfUp(value: Long, multiplier: Long, divisor: Long): Long {
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

    private fun invalid(message: String): Nothing = throw BusinessFinanceValidationException(message)
}

data class SalesDocumentDraftInputWithTotals(
    val input: SalesDocumentDraftInput,
    val lines: List<CalculatedSalesLine>,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

data class CalculatedFinanceEntryInput(
    val input: FinanceEntryInput,
    val taxCents: Long,
    val grossCents: Long,
)
