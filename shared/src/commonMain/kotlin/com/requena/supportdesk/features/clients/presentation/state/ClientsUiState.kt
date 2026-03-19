package com.requena.supportdesk.features.clients.presentation.state

import com.requena.supportdesk.core.model.Client

data class ClientsUiState(
    val clients: List<Client> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
