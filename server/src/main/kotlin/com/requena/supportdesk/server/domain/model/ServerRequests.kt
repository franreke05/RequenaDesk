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
    val billing: ClientBillingRequest = ClientBillingRequest(),
)

/** Fiscal data of a client (recipient of its invoices). Every field is optional. */
@Serializable
data class ClientBillingRequest(
    val legalName: String = "",
    val taxId: String = "",
    val addressLine: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val email: String = "",
) {
    fun normalized(): ClientBillingRequest = ClientBillingRequest(
        legalName = legalName.trim(),
        taxId = taxId.uppercase().filter { it.isLetterOrDigit() },
        addressLine = addressLine.trim(),
        postalCode = postalCode.trim(),
        city = city.trim(),
        province = province.trim(),
        country = country.trim(),
        email = email.trim(),
    )

    /** Why the normalized data cannot be stored, or null when it is valid. */
    fun problem(): String? = when {
        taxId.isNotEmpty() && !CLIENT_TAX_ID_PATTERN.matches(taxId) -> "Invalid client tax id"
        email.isNotEmpty() && (!email.contains("@") || email.length > 254) -> "Invalid client billing email"
        legalName.length > 200 || addressLine.length > 250 || postalCode.length > 12 ||
            city.length > 120 || province.length > 120 || country.length > 80 -> "Client billing data is too long"
        else -> null
    }

    fun toSnapshot(): ServerClientBilling = ServerClientBilling(
        legalName = legalName,
        taxId = taxId,
        addressLine = addressLine,
        postalCode = postalCode,
        city = city,
        province = province,
        country = country,
        email = email,
    )
}

private val CLIENT_TAX_ID_PATTERN = Regex("^[A-Z0-9]{5,20}$")

@Serializable
data class UpdateClientRequest(
    val companyName: String? = null,
    val productName: String? = null,
    val contactName: String? = null,
    val email: String? = null,
    val accountStatus: String? = null,
    val serviceTier: String? = null,
    val preferredContactChannel: String? = null,
    /** When present, replaces all fiscal data of the client (blank fields clear it). */
    val billing: ClientBillingRequest? = null,
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
data class CreateClientProgramRequestsRequest(
    val productKeys: List<String> = emptyList(),
    val customerNote: String = "",
)

@Serializable
data class ApproveClientProgramRequest(
    val monthlyPriceCents: Long = 0,
    val adminNote: String? = null,
)

@Serializable
data class RejectClientProgramRequest(
    val adminNote: String? = null,
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
data class UpdateTaskBoardPlacementRequest(
    val boardDate: String = "",
    val positionX: Double = Double.NaN,
    val positionY: Double = Double.NaN,
)

@Serializable
data class StartWorkSessionRequest(
    val taskId: String = "",
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
