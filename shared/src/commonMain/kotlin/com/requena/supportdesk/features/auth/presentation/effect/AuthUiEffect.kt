package com.requena.supportdesk.features.auth.presentation.effect

sealed interface AuthUiEffect {
    object NavigateToHome : AuthUiEffect
    data class ShowMessage(val message: String) : AuthUiEffect
}
