package com.requena.supportdesk.features.clients.domain.usecase

import com.requena.supportdesk.features.clients.domain.repository.ClientsRepository

class GetClientsUseCase(
    private val repository: ClientsRepository,
) {
    suspend operator fun invoke() = repository.getClients()
}
