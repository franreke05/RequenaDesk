package com.requena.supportdesk.features.clients.presentation.state

import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.features.clients.domain.model.ClientAccessCredentials

data class ClientsUiState(
    val clients: List<Client> = emptyList(),
    val selectedClientId: String? = null,
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastCreatedClientId: String? = null,
    val generatedCredentials: ClientAccessCredentials? = null,
) {
    val selectedClient: Client?
        get() = clients.firstOrNull { it.id == selectedClientId } ?: clients.firstOrNull()
}
