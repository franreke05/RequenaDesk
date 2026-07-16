package com.requena.supportdesk.features.clients.domain.repository

import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.clients.domain.model.ClientDraft
import com.requena.supportdesk.features.clients.domain.model.ClientCredentialsDraft

interface ClientsRepository {
    suspend fun getClients(): AppResult<List<Client>>
    suspend fun createClient(input: ClientDraft): AppResult<Client>
    suspend fun updateClient(clientId: String, input: ClientDraft): AppResult<Client>
    suspend fun updateClientCredentials(clientId: String, input: ClientCredentialsDraft): AppResult<Unit>
    suspend fun deleteClient(clientId: String): AppResult<Unit>
}
