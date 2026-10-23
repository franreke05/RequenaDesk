package com.requena.supportdesk.core.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val clientId: String? = null,
    // Company (phase 2). Staff users have a company role; portal users only the company id.
    val organizationId: String? = null,
    val organizationRole: OrganizationRole? = null,
    val organizationName: String? = null,
)
