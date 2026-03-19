package com.requena.supportdesk.server.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String = "",
    val password: String = "",
)

@Serializable
data class CreateTicketRequest(
    val clientId: String = "client-1",
    val subject: String = "",
    val description: String = "",
    val category: String = "QUESTION",
    val affectedApp: String = "",
    val platform: String = "DESKTOP",
    val appVersion: String? = null,
    val stepsToReproduce: String? = null,
    val clientReference: String? = null,
    val priority: String = "MEDIUM",
)

@Serializable
data class UpdateTicketStatusRequest(
    val status: String = "IN_PROGRESS",
)

@Serializable
data class UpdateTicketPriorityRequest(
    val priority: String = "HIGH",
)

@Serializable
data class CreateClientRequest(
    val companyName: String = "",
    val productName: String = "",
    val email: String = "",
    val accountStatus: String = "ACTIVE",
    val serviceTier: String = "STANDARD",
    val preferredContactChannel: String = "TICKET",
)

@Serializable
data class RegisterDeviceRequest(
    val userId: String = "user-admin",
    val platform: String = "ANDROID",
)
