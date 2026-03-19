package com.requena.supportdesk.features.clients.presentation.effect

sealed interface ClientsUiEffect {
    data class ShowMessage(val message: String) : ClientsUiEffect
}
