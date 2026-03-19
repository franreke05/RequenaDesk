package com.requena.supportdesk.features.clients.presentation.event

sealed interface ClientsUiEvent {
    object Load : ClientsUiEvent
    data class SearchChanged(val query: String) : ClientsUiEvent
}
