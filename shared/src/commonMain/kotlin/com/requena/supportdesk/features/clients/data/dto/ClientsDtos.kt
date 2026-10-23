package com.requena.supportdesk.features.clients.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClientDto(
    val id: String,
    val ownerAdminId: String,
    val companyName: String,
    val contactName: String = "",
    val email: String,
    val productName: String = "",
    val accountStatus: String = "ACTIVE",
    val serviceTier: String = "STANDARD",
    val enabledComponents: List<String> = emptyList(),
    val preferredContactChannel: String = "TICKET",
    val activeTicketCount: Int = 0,
    val openTasksCount: Int = 0,
    val monthlyLoggedMinutes: Int = 0,
    val generatedAccessCode: String? = null,
    val billing: ClientBillingDto = ClientBillingDto(),
)

@Serializable
data class ClientBillingDto(
    val legalName: String = "",
    val taxId: String = "",
    val addressLine: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val email: String = "",
)

@Serializable
data class ClientAccessCredentialsDto(
    val clientId: String,
    val email: String,
    val accessCode: String,
)

@Serializable
data class CreateClientRequestDto(
    val companyName: String,
    val productName: String,
    val contactName: String,
    val email: String,
    val accountStatus: String,
    val serviceTier: String,
    val preferredContactChannel: String,
    val billing: ClientBillingDto = ClientBillingDto(),
)

@Serializable
data class UpdateClientRequestDto(
    val companyName: String,
    val productName: String,
    val contactName: String,
    val email: String,
    val accountStatus: String,
    val serviceTier: String,
    val preferredContactChannel: String,
    // Required (no default) so it is always encoded: the server replaces all fiscal data with it.
    val billing: ClientBillingDto,
)

@Serializable
data class UpdateClientCredentialsRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class UpdateClientComponentsRequestDto(
    val components: List<String>,
)
