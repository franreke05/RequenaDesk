package com.requena.supportdesk.features.auth.data.datasource

import com.requena.supportdesk.core.common.SUPPORT_DESK_ADMIN_EMAIL
import com.requena.supportdesk.core.common.SUPPORT_DESK_CLIENT_EMAIL
import com.requena.supportdesk.features.auth.data.dto.AuthSessionDto

interface AuthDataSource {
    suspend fun login(email: String, password: String): AuthSessionDto
}

class StubAuthDataSource : AuthDataSource {
    override suspend fun login(email: String, password: String): AuthSessionDto {
        return if (email.equals(SUPPORT_DESK_ADMIN_EMAIL, ignoreCase = true)) {
            AuthSessionDto(
                id = "user-admin",
                name = "Fran Requena",
                email = SUPPORT_DESK_ADMIN_EMAIL,
                role = "ADMIN",
            )
        } else {
            AuthSessionDto(
                id = "user-client",
                name = "Cliente Demo",
                email = if (email.isBlank()) SUPPORT_DESK_CLIENT_EMAIL else email,
                role = "CLIENT",
                clientId = "client-1",
            )
        }
    }
}
