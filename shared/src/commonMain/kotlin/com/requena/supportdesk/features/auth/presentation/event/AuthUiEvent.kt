package com.requena.supportdesk.features.auth.presentation.event

sealed interface AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    object Submit : AuthUiEvent
    object Logout : AuthUiEvent
}
