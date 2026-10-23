package com.requena.supportdesk.core.model

data class Organization(
    val id: String,
    val name: String,
    val legalForm: LegalForm? = null,
    val sector: String = "",
    val teamSize: String = "",
    val hasAccountant: Boolean? = null,
    val onboardingGoals: Set<OnboardingGoal> = emptySet(),
    val onboardingCompleted: Boolean = false,
    val legalName: String = "",
    val taxId: String = "",
    val addressLine: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "ES",
)

enum class LegalForm(val label: String) {
    AUTONOMO("Autónomo/a"),
    SOCIEDAD("Sociedad"),
}

/** Answers to "¿Qué quieres resolver?" in onboarding. Keys match the server's ONBOARDING_GOALS. */
enum class OnboardingGoal(val label: String, val description: String) {
    INVOICING("Cobrar y facturar", "Presupuestos, facturas y gastos."),
    WORK_TIME("Organizar trabajo y horas", "Tareas, temporizador y tablero diario."),
    SUPPORT("Atender a mis clientes", "Tickets, SLA y portal de cliente."),
    BOOKINGS("Agenda y citas", "Reservas y disponibilidad."),
    DOCUMENTS("Documentos", "Carpetas, versiones y confirmaciones."),
    ;

    companion object {
        fun fromKey(key: String): OnboardingGoal? = entries.firstOrNull { it.name == key.trim().uppercase() }
    }
}

data class OrganizationMember(
    val userId: String,
    val name: String,
    val email: String,
    val role: OrganizationRole,
    val isActive: Boolean,
)

data class OrganizationInvitation(
    val id: String,
    val role: OrganizationRole,
    val status: InvitationStatus,
    val createdByName: String,
    val expiresAt: String,
)

enum class InvitationStatus(val label: String) {
    PENDING("Pendiente"),
    ACCEPTED("Aceptada"),
    REVOKED("Revocada"),
    EXPIRED("Caducada"),
    ;

    companion object {
        fun fromKey(key: String): InvitationStatus = entries.firstOrNull { it.name == key } ?: EXPIRED
    }
}

/** A just-created invitation: the plain code is only available here, once. */
data class CreatedInvitation(val invitation: OrganizationInvitation, val code: String)
