package com.requena.supportdesk.core.model

/** Role of a staff user inside their company. Mirrors the server's organization_members.role. */
enum class OrganizationRole(val label: String, val description: String) {
    OWNER("Dueño/a", "Todo, incluido gestionar a otros dueños."),
    ADMIN("Admin", "Todo menos tocar a los dueños."),
    MEMBER("Miembro", "Trabaja con normalidad; no gestiona la empresa ni el equipo."),
    ACCOUNTANT("Gestoría", "Lo ve todo pero no puede cambiar nada."),
    ;

    /** Can edit the company data and manage members and invitations. */
    val canManageCompany: Boolean get() = this == OWNER || this == ADMIN

    /** Gestoría: read-only everywhere. */
    val isReadOnly: Boolean get() = this == ACCOUNTANT

    companion object {
        fun fromKey(key: String?): OrganizationRole? = entries.firstOrNull { it.name == key?.trim()?.uppercase() }

        /** Roles an owner/admin can hand out with an invitation code. */
        val invitable = listOf(MEMBER, ADMIN, ACCOUNTANT)
    }
}

/** Effective permissions of the signed-in user, used by the UI to show or disable actions. */
val User.isStaff: Boolean get() = role == UserRole.ADMIN
val User.canManageCompany: Boolean get() = isStaff && organizationRole?.canManageCompany == true
val User.isReadOnly: Boolean get() = organizationRole?.isReadOnly == true
