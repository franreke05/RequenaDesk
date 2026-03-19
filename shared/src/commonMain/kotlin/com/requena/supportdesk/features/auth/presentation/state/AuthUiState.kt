package com.requena.supportdesk.features.auth.presentation.state

import com.requena.supportdesk.core.common.SUPPORT_DESK_ADMIN_EMAIL
import com.requena.supportdesk.core.common.SUPPORT_DESK_DEFAULT_PASSWORD
import com.requena.supportdesk.core.model.User

data class AuthUiState(
    val email: String = SUPPORT_DESK_ADMIN_EMAIL,
    val password: String = SUPPORT_DESK_DEFAULT_PASSWORD,
    val isLoading: Boolean = false,
    val authenticatedUser: User? = null,
    val errorMessage: String? = null,
)
