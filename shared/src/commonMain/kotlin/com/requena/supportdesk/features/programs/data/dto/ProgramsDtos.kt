package com.requena.supportdesk.features.programs.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClientProgramDto(
    val key: String,
    val name: String,
    val shortDescription: String = "",
    val category: String = "PRODUCTIVITY",
    val iconKey: String = "APP_WINDOW",
    val monthlyPriceCents: Long = 0,
    val currency: String = "EUR",
    val isRequestable: Boolean = false,
    val isAvailable: Boolean = false,
    val capabilities: List<String> = emptyList(),
)

@Serializable
data class ClientProgramSubscriptionDto(
    val productKey: String,
    val status: String,
    val monthlyPriceCents: Long,
    val currency: String = "EUR",
    val startsOn: String,
    val endsOn: String? = null,
)

@Serializable
data class ClientProgramRequestDto(
    val id: String,
    val productKey: String,
    val status: String,
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

@Serializable
data class ClientProgramsOverviewDto(
    val catalog: List<ClientProgramDto> = emptyList(),
    val subscriptions: List<ClientProgramSubscriptionDto> = emptyList(),
    val requests: List<ClientProgramRequestDto> = emptyList(),
)

@Serializable
data class CreateProgramRequestsRequestDto(
    val productKeys: List<String>,
    val customerNote: String = "",
)

@Serializable
data class DecideProgramRequestDto(
    val monthlyPriceCents: Long? = null,
    val adminNote: String? = null,
)

@Serializable
data class ClientProgramBillingLineDto(
    val productKey: String,
    val name: String,
    val monthlyPriceCents: Long,
    val currency: String = "EUR",
)

@Serializable
data class ClientProgramBillingPreviewDto(
    val clientId: String,
    val period: String,
    val lines: List<ClientProgramBillingLineDto> = emptyList(),
    val totalMonthlyPriceCents: Long,
    val currency: String = "EUR",
)
