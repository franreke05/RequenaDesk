package com.requena.supportdesk.features.auth.presentation.event

sealed interface AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    data class ClientEmailChanged(val value: String) : AuthUiEvent
    data class ClientAccessCodeChanged(val value: String) : AuthUiEvent
    object ClaimClientAccess : AuthUiEvent
    object Submit : AuthUiEvent
    object Logout : AuthUiEvent
}
