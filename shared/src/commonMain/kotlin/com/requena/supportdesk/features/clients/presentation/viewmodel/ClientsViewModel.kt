package com.requena.supportdesk.features.clients.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.core.utils.matchesQuery
import com.requena.supportdesk.features.clients.domain.model.ClientDraft
import com.requena.supportdesk.features.clients.domain.model.ClientCredentialsDraft
import com.requena.supportdesk.features.clients.domain.usecase.CreateClientUseCase
import com.requena.supportdesk.features.clients.domain.usecase.DeleteClientUseCase
import com.requena.supportdesk.features.clients.domain.usecase.GetClientsUseCase
import com.requena.supportdesk.features.clients.domain.usecase.UpdateClientUseCase
import com.requena.supportdesk.features.clients.domain.usecase.UpdateClientCredentialsUseCase
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
    private val createClientUseCase: CreateClientUseCase,
    private val updateClientUseCase: UpdateClientUseCase,
    private val updateClientCredentialsUseCase: UpdateClientCredentialsUseCase,
    private val deleteClientUseCase: DeleteClientUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(ClientsUiState())
    val state: StateFlow<ClientsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ClientsUiEffect>(extraBufferCapacity = 2)
    val effects: SharedFlow<ClientsUiEffect> = _effects.asSharedFlow()

    private var sourceClients = emptyList<Client>()

    init {
        onEvent(ClientsUiEvent.Load)
    }

    fun onEvent(event: ClientsUiEvent) {
        when (event) {
            ClientsUiEvent.Load -> loadClients()
            is ClientsUiEvent.SearchChanged -> {
                _state.update { it.copy(query = event.query) }
                renderClients()
            }
            is ClientsUiEvent.SelectClient -> _state.update { it.copy(selectedClientId = event.clientId) }
            is ClientsUiEvent.CreateClient -> createClient(event)
            is ClientsUiEvent.UpdateClient -> updateClient(event)
            is ClientsUiEvent.UpdateClientCredentials -> updateClientCredentials(event)
            is ClientsUiEvent.DeleteClient -> deleteClient(event.clientId)
            is ClientsUiEvent.AddClientNote -> {
                // Notes are out of scope for the current remote CRUD flow.
            }
        }
    }

    private fun loadClients(
        preferredSelectedId: String? = state.value.selectedClientId,
        successMessage: String? = null,
    ) {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getClientsUseCase()) {
                is AppResult.Error -> {
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = result.message,
                        )
                    }
                    _effects.emit(ClientsUiEffect.ShowMessage(result.message))
                    renderClients(preferredSelectedId = preferredSelectedId)
                }
                is AppResult.Success -> {
                    sourceClients = result.data
                    renderClients(preferredSelectedId = preferredSelectedId)
                    if (successMessage != null) {
                        _effects.emit(ClientsUiEffect.ShowMessage(successMessage))
                    }
                }
            }
        }
    }

    private fun createClient(event: ClientsUiEvent.CreateClient) {
        val draft = event.toDraft()
        if (!draft.isValid()) return
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, lastCreatedClientId = null) }
            when (val result = createClientUseCase(draft)) {
                is AppResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                    _effects.emit(ClientsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    _state.update { it.copy(lastCreatedClientId = result.data.id) }
                    loadClients(
                        preferredSelectedId = result.data.id,
                        successMessage = "Cliente creado",
                    )
                }
            }
        }
    }

    private fun updateClient(event: ClientsUiEvent.UpdateClient) {
        val draft = event.toDraft()
        if (!draft.isValid()) return
        launch {
            when (val result = updateClientUseCase(event.clientId, draft)) {
                is AppResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                    _effects.emit(ClientsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> loadClients(
                    preferredSelectedId = result.data.id,
                    successMessage = "Cliente actualizado",
                )
            }
        }
    }

    private fun updateClientCredentials(event: ClientsUiEvent.UpdateClientCredentials) {
        val draft = ClientCredentialsDraft(
            email = event.email.trim(),
            password = event.password,
        )
        if (draft.email.isBlank() || draft.password.length < 8) {
            launch { _effects.emit(ClientsUiEffect.ShowMessage("Indica un correo y una contrasena de al menos 8 caracteres.")) }
            return
        }
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = updateClientCredentialsUseCase(event.clientId, draft)) {
                is AppResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                    _effects.emit(ClientsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _effects.emit(ClientsUiEffect.ShowMessage("Credenciales de acceso actualizadas"))
                }
            }
        }
    }

    private fun deleteClient(clientId: String) {
        val fallbackSelection = nextSelectionAfterDelete(clientId)
        launch {
            when (val result = deleteClientUseCase(clientId)) {
                is AppResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                    _effects.emit(ClientsUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> loadClients(
                    preferredSelectedId = fallbackSelection,
                    successMessage = "Cliente borrado",
                )
            }
        }
    }

    private fun renderClients(preferredSelectedId: String? = state.value.selectedClientId) {
        val query = state.value.query
        val filteredClients = sourceClients.filter { client ->
            client.companyName.matchesQuery(query) ||
                client.productName.matchesQuery(query) ||
                client.contactName.matchesQuery(query) ||
                client.email.matchesQuery(query)
        }
        _state.update { current ->
            current.copy(
                clients = filteredClients,
                isLoading = false,
                selectedClientId = when {
                    filteredClients.isEmpty() -> null
                    preferredSelectedId != null && filteredClients.any { it.id == preferredSelectedId } -> preferredSelectedId
                    current.selectedClientId != null && filteredClients.any { it.id == current.selectedClientId } -> current.selectedClientId
                    else -> filteredClients.first().id
                },
            )
        }
    }

    private fun nextSelectionAfterDelete(clientId: String): String? {
        val clients = state.value.clients
        val index = clients.indexOfFirst { it.id == clientId }
        if (index < 0) return state.value.selectedClientId
        return clients.getOrNull(index + 1)?.id ?: clients.getOrNull(index - 1)?.id
    }

    private fun ClientsUiEvent.CreateClient.toDraft(): ClientDraft = ClientDraft(
        companyName = companyName.trim(),
        productName = productName.trim(),
        contactName = contactName.trim(),
        email = email.trim(),
        accountStatus = accountStatus,
        serviceTier = serviceTier,
        preferredContactChannel = preferredContactChannel,
    )

    private fun ClientsUiEvent.UpdateClient.toDraft(): ClientDraft = ClientDraft(
        companyName = companyName.trim(),
        productName = productName.trim(),
        contactName = contactName.trim(),
        email = email.trim(),
        accountStatus = accountStatus,
        serviceTier = serviceTier,
        preferredContactChannel = preferredContactChannel,
    )

    private fun ClientDraft.isValid(): Boolean =
        companyName.isNotBlank() &&
            productName.isNotBlank() &&
            contactName.isNotBlank() &&
            email.isNotBlank()
}
