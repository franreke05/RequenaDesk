package com.requena.supportdesk.features.auth.data.mapper

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto

object AuthMapper {
    fun fromDto(dto: AuthSessionDto): User = User(
        id = dto.userId,
        name = dto.name,
        email = dto.email,
        role = if (dto.role == "ADMIN") UserRole.ADMIN else UserRole.CLIENT,
        clientId = dto.clientId,
        companyName = dto.companyName,
    )
}
