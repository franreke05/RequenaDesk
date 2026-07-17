package com.requena.supportdesk.server.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String = "",
    val password: String = "",
)

@Serializable
data class RefreshSessionRequest(
    val refreshToken: String = "",
)

@Serializable
data class LogoutRequest(
    val refreshToken: String = "",
)

@Serializable
data class CreateTicketRequest(
    val clientId: String = "client-1",
    val requesterId: String? = null,
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
data class CreateTicketMessageRequest(
    val authorId: String = "",
    val body: String = "",
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
    val contactName: String = "",
    val email: String = "",
    val accountStatus: String = "ACTIVE",
    val serviceTier: String = "STANDARD",
    val preferredContactChannel: String = "TICKET",
)

@Serializable
data class UpdateClientRequest(
    val companyName: String? = null,
    val productName: String? = null,
    val contactName: String? = null,
    val email: String? = null,
    val accountStatus: String? = null,
    val serviceTier: String? = null,
    val preferredContactChannel: String? = null,
)

@Serializable
data class UpdateClientCredentialsRequest(
    val email: String = "",
    val password: String = "",
)

@Serializable
data class UpdateClientComponentsRequest(
    val components: List<String> = emptyList(),
)

@Serializable
data class CreateClientContactRequest(
    val fullName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val isPrimary: Boolean = false,
)

@Serializable
data class UpdateClientContactRequest(
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val isPrimary: Boolean? = null,
)

@Serializable
data class CreateClientActivityRequest(
    val type: String = "NOTE",
    val subject: String = "",
    val details: String? = null,
    val contactId: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class UpdateClientActivityRequest(
    val type: String? = null,
    val subject: String? = null,
    val details: String? = null,
    val contactId: String? = null,
    val dueDate: String? = null,
    val completed: Boolean? = null,
)

@Serializable
data class CreateTaskLabelRequest(
    val ownerAdminId: String = "",
    val name: String = "",
    val colorHex: String = "#6B7A5B",
)

@Serializable
data class UpdateTaskLabelRequest(
    val name: String? = null,
    val colorHex: String? = null,
)

@Serializable
data class CreateTaskRequest(
    val title: String = "",
    val description: String = "",
    val clientId: String? = null,
    val labelId: String = "",
    val dueDate: String? = null,
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val clientId: String? = null,
    val labelId: String? = null,
    val dueDate: String? = null,
    val completed: Boolean? = null,
)

@Serializable
data class CreateTimeLogRequest(
    val taskId: String = "",
    val authorId: String = "",
    val workDate: String = "",
    val minutes: Int = 0,
    val seconds: Int = minutes * 60,
    val note: String = "",
    val billable: Boolean = false,
)

@Serializable
data class UploadAttachmentRequest(
    val uploadedBy: String = "",
    val fileName: String = "",
    val contentType: String = "application/octet-stream",
    val storageKey: String = "",
    val sizeBytes: Long = 0,
    val messageId: String? = null,
)

@Serializable
data class RegisterDeviceRequest(
    val userId: String = "user-admin",
    val token: String = "",
    val platform: String = "ANDROID",
)
