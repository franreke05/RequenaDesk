package com.requena.supportdesk.features.clients.data.repository

import com.requena.supportdesk.core.common.SupportDeskSeed
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.clients.domain.repository.ClientsRepository

class FakeClientsRepository : ClientsRepository {
    override suspend fun getClients(): AppResult<List<Client>> = AppResult.Success(SupportDeskSeed.clients)
}
