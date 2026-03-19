package com.requena.supportdesk.features.clients.data.datasource

import com.requena.supportdesk.features.clients.data.dto.ClientDto

interface ClientsDataSource {
    suspend fun getClients(): List<ClientDto>
}
