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
