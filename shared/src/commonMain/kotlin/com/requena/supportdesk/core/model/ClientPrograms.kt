package com.requena.supportdesk.core.model

/**
 * Server-owned catalog item. The client renders it but Ktor decides whether a
 * request can be created or a utility can be opened.
 */
data class ClientProgram(
    val key: String,
    val name: String,
    val shortDescription: String,
    val category: String,
    val iconKey: String,
    val monthlyPriceCents: Long,
    val currency: String = "EUR",
    val isRequestable: Boolean,
    val isAvailable: Boolean,
    val capabilities: List<String> = emptyList(),
)

enum class ClientProgramSubscriptionStatus {
    ACTIVE,
    SUSPENDED,
    CANCELLED,
}

enum class ProgramRequestStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    CANCELLED,
}

data class ClientProgramSubscription(
    val productKey: String,
    val status: ClientProgramSubscriptionStatus,
    val monthlyPriceCents: Long,
    val currency: String,
    val startsOn: String,
    val endsOn: String? = null,
)

data class ClientProgramRequest(
    val id: String,
    val productKey: String,
    val status: ProgramRequestStatus,
    val customerNote: String? = null,
    val adminNote: String? = null,
    val requestedAt: String,
    val decidedAt: String? = null,
    val quotedMonthlyPriceCents: Long? = null,
    val currency: String = "EUR",
    val clientId: String? = null,
    val clientCompanyName: String? = null,
    val requestedByName: String? = null,
)

data class ClientProgramsOverview(
    val catalog: List<ClientProgram> = emptyList(),
    val subscriptions: List<ClientProgramSubscription> = emptyList(),
    val requests: List<ClientProgramRequest> = emptyList(),
)

data class ClientProgramBillingLine(
    val productKey: String,
    val name: String,
    val monthlyPriceCents: Long,
    val currency: String,
)

data class ClientProgramBillingPreview(
    val clientId: String,
    val period: String,
    val lines: List<ClientProgramBillingLine>,
    val totalMonthlyPriceCents: Long,
    val currency: String,
)

fun Long.asProgramMoney(currency: String = "EUR"): String {
    val symbol = if (currency == "EUR") "€" else currency
    val absoluteValue = kotlin.math.abs(this)
    val formatted = "${absoluteValue / 100},${(absoluteValue % 100).toString().padStart(2, '0')}"
    return if (this < 0) "−$formatted $symbol" else "$formatted $symbol"
}
