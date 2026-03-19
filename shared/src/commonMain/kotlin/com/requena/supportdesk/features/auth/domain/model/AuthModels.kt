package com.requena.supportdesk.features.auth.domain.model

import com.requena.supportdesk.core.model.User

data class LoginCredentials(
    val email: String = "",
    val password: String = "",
)

data class AuthSession(
    val user: User,
)
