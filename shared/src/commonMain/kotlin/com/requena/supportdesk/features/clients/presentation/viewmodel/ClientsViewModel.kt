package com.requena.supportdesk.features.clients.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.core.utils.matchesQuery
import com.requena.supportdesk.features.clients.domain.usecase.GetClientsUseCase
import com.requena.supportdesk.features.clients.presentation.effect.ClientsUiEffect
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent
import com.requena.supportdesk.features.clients.presentation.state.ClientsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ClientsViewModel(
    private val getClientsUseCase: GetClientsUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(ClientsUiState())
    val state: StateFlow<ClientsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ClientsUiEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ClientsUiEffect> = _effects.asSharedFlow()

    private var allClients = emptyList<com.requena.supportdesk.core.model.Client>()

    init {
        onEvent(ClientsUiEvent.Load)
    }

    fun onEvent(event: ClientsUiEvent) {
        when (event) {
            ClientsUiEvent.Load -> loadClients()
            is ClientsUiEvent.SearchChanged -> {
                _state.update { it.copy(query = event.query) }
                applyFilter()
            }
        }
    }

    private fun loadClients() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getClientsUseCase()) {
                is AppResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                    _effects.emit(ClientsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    allClients = result.data
                    _state.update { it.copy(isLoading = false) }
                    applyFilter()
                }
            }
        }
    }

    private fun applyFilter() {
        val query = state.value.query
        _state.update {
            it.copy(clients = allClients.filter { client ->
                client.companyName.matchesQuery(query) ||
                    client.productName.matchesQuery(query) ||
                    client.contactName.matchesQuery(query) ||
                    client.email.matchesQuery(query)
            })
        }
    }
}
