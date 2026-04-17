package com.requena.supportdesk.features.clients.data.datasource

import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.requireSuccess
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.clients.data.dto.ClientDto
import com.requena.supportdesk.features.clients.data.dto.CreateClientRequestDto
import com.requena.supportdesk.features.clients.data.dto.UpdateClientRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface ClientsDataSource {
    suspend fun getClients(): List<ClientDto>
    suspend fun createClient(request: CreateClientRequestDto): ClientDto
    suspend fun updateClient(clientId: String, request: UpdateClientRequestDto): ClientDto
    suspend fun deleteClient(clientId: String)
}

class RemoteClientsDataSource(
    private val httpClient: HttpClient,
) : ClientsDataSource {
    override suspend fun getClients(): List<ClientDto> =
        httpClient.get("${supportDeskBaseUrl()}/admin/clients").requireApiData()

    override suspend fun createClient(request: CreateClientRequestDto): ClientDto =
        httpClient.post("${supportDeskBaseUrl()}/admin/clients") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun updateClient(clientId: String, request: UpdateClientRequestDto): ClientDto =
        httpClient.patch("${supportDeskBaseUrl()}/admin/clients/$clientId") {
            setBody(jsonRequestBody(request))
        }.requireApiData()

    override suspend fun deleteClient(clientId: String) {
        httpClient.delete("${supportDeskBaseUrl()}/admin/clients/$clientId").requireSuccess()
    }
}
