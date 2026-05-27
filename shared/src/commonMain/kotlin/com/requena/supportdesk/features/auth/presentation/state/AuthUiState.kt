package com.requena.supportdesk.features.auth.presentation.state

import com.requena.supportdesk.core.model.User

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val clientEmail: String = "",
    val clientAccessCode: String = "",
    val isLoading: Boolean = false,
    val authenticatedUser: User? = null,
    val errorMessage: String? = null,
)
