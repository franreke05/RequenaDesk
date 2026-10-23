package com.requena.supportdesk.features.auth.presentation.state

import com.requena.supportdesk.core.model.User

/** Which form the access screen shows. */
enum class AuthMode {
    LOGIN,

    /** Create a new company (only when the server has signup enabled). */
    SIGNUP,

    /** Join an existing company with an invitation code. */
    JOIN,
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val organizationName: String = "",
    val inviteCode: String = "",
    val acceptTerms: Boolean = false,
    val acceptPrivacy: Boolean = false,
    val signupAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val authenticatedUser: User? = null,
    val errorMessage: String? = null,
)
