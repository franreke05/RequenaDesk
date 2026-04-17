package com.requena.supportdesk.features.clients.domain.usecase

import com.requena.supportdesk.features.clients.domain.model.ClientDraft
import com.requena.supportdesk.features.clients.domain.repository.ClientsRepository

class GetClientsUseCase(
    private val repository: ClientsRepository,
) {
    suspend operator fun invoke() = repository.getClients()
}

class CreateClientUseCase(
    private val repository: ClientsRepository,
) {
    suspend operator fun invoke(input: ClientDraft) = repository.createClient(input)
}

class UpdateClientUseCase(
    private val repository: ClientsRepository,
) {
    suspend operator fun invoke(clientId: String, input: ClientDraft) = repository.updateClient(clientId, input)
}

class DeleteClientUseCase(
    private val repository: ClientsRepository,
) {
    suspend operator fun invoke(clientId: String) = repository.deleteClient(clientId)
}
