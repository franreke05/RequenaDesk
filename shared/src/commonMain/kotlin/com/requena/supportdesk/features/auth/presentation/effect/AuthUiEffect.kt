package com.requena.supportdesk.features.auth.presentation.effect

import com.requena.supportdesk.core.model.UserRole

sealed interface AuthUiEffect {
    data class NavigateToHome(val role: UserRole) : AuthUiEffect
    data class ShowMessage(val message: String) : AuthUiEffect
}
