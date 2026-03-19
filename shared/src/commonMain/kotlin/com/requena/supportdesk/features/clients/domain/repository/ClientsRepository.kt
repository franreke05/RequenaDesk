package com.requena.supportdesk.features.clients.domain.repository

import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.result.AppResult

interface ClientsRepository {
    suspend fun getClients(): AppResult<List<Client>>
}
