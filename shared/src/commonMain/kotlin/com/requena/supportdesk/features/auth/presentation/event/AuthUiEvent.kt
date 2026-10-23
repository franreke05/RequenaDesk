package com.requena.supportdesk.features.auth.presentation.event

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.features.auth.presentation.state.AuthMode

sealed interface AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    data class NameChanged(val value: String) : AuthUiEvent
    data class OrganizationNameChanged(val value: String) : AuthUiEvent
    data class InviteCodeChanged(val value: String) : AuthUiEvent
    data class AcceptTermsChanged(val value: Boolean) : AuthUiEvent
    data class AcceptPrivacyChanged(val value: Boolean) : AuthUiEvent
    data class ModeChanged(val mode: AuthMode) : AuthUiEvent
    object Submit : AuthUiEvent
    object Logout : AuthUiEvent

    /** The session user changed elsewhere (e.g. refreshed with a new company role). */
    data class SessionUserChanged(val user: User) : AuthUiEvent
}
