package com.requena.supportdesk.features.clients.presentation.state

import com.requena.supportdesk.core.model.Client

data class ClientsUiState(
    val clients: List<Client> = emptyList(),
    val selectedClientId: String? = null,
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val generatedCode: String? = null,
    val isGeneratingCode: Boolean = false,
) {
    val selectedClient: Client?
        get() = clients.firstOrNull { it.id == selectedClientId } ?: clients.firstOrNull()
}
