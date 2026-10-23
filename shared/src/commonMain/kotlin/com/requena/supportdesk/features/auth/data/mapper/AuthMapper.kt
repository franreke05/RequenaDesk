package com.requena.supportdesk.features.auth.data.mapper

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.OrganizationRole
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto

object AuthMapper {
    fun fromDto(dto: AuthSessionDto): User = User(
        id = dto.userId,
        name = dto.name,
        email = dto.email,
        // Unknown roles are rejected instead of silently becoming CLIENT: a stored session
        // with an unexpected role is discarded and the user simply logs in again.
        role = when (dto.role) {
            "ADMIN" -> UserRole.ADMIN
            "CLIENT" -> UserRole.CLIENT
            else -> throw IllegalArgumentException("Unknown user role: ${dto.role}")
        },
        clientId = dto.clientId,
        organizationId = dto.organizationId,
        organizationRole = OrganizationRole.fromKey(dto.organizationRole),
        organizationName = dto.organizationName,
    )
}
