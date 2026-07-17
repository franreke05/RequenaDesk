package com.requena.supportdesk.features.clients.data.repository

import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.clients.data.datasource.ClientsDataSource
import com.requena.supportdesk.features.clients.data.dto.CreateClientRequestDto
import com.requena.supportdesk.features.clients.data.dto.UpdateClientRequestDto
import com.requena.supportdesk.features.clients.data.dto.UpdateClientCredentialsRequestDto
import com.requena.supportdesk.features.clients.data.dto.UpdateClientComponentsRequestDto
import com.requena.supportdesk.core.model.ClientPortalComponent
import com.requena.supportdesk.features.clients.data.mapper.ClientsMapper
import com.requena.supportdesk.features.clients.domain.model.ClientDraft
import com.requena.supportdesk.features.clients.domain.model.ClientCredentialsDraft
import com.requena.supportdesk.features.clients.domain.model.ClientAccessCredentials
import com.requena.supportdesk.features.clients.domain.model.ClientProvisioningResult
import com.requena.supportdesk.features.clients.domain.repository.ClientsRepository

class ClientsRepositoryImpl(
    private val dataSource: ClientsDataSource,
) : ClientsRepository {
    override suspend fun getClients(): AppResult<List<Client>> = runCatching {
        dataSource.getClients().map(ClientsMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudieron cargar los clientes.", cause = it) },
    )

    override suspend fun createClient(input: ClientDraft): AppResult<ClientProvisioningResult> = runCatching {
        dataSource.createClient(
            CreateClientRequestDto(
                companyName = input.companyName,
                productName = input.productName,
                contactName = input.contactName,
                email = input.email,
                accountStatus = input.accountStatus.name,
                serviceTier = input.serviceTier.name,
                preferredContactChannel = input.preferredContactChannel.name,
            ),
        ).let { clientDto ->
            val accessCode = requireNotNull(clientDto.generatedAccessCode) { "El servidor no devolvió la clave de acceso." }
            ClientProvisioningResult(
                client = ClientsMapper.fromDto(clientDto),
                credentials = ClientAccessCredentials(clientDto.id, clientDto.email, accessCode),
            )
        }
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo crear el cliente.", cause = it) },
    )

    override suspend fun updateClient(clientId: String, input: ClientDraft): AppResult<Client> = runCatching {
        dataSource.updateClient(
            clientId = clientId,
            request = UpdateClientRequestDto(
                companyName = input.companyName,
                productName = input.productName,
                contactName = input.contactName,
                email = input.email,
                accountStatus = input.accountStatus.name,
                serviceTier = input.serviceTier.name,
                preferredContactChannel = input.preferredContactChannel.name,
            ),
        ).let(ClientsMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo actualizar el cliente.", cause = it) },
    )

    override suspend fun updateClientCredentials(clientId: String, input: ClientCredentialsDraft): AppResult<Unit> = runCatching {
        dataSource.updateClientCredentials(
            clientId = clientId,
            request = UpdateClientCredentialsRequestDto(
                email = input.email,
                password = input.password,
            ),
        )
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudieron guardar las credenciales.", cause = it) },
    )

    override suspend fun regenerateClientCredentials(clientId: String): AppResult<ClientAccessCredentials> = runCatching {
        dataSource.regenerateClientCredentials(clientId).let { credentials ->
            ClientAccessCredentials(credentials.clientId, credentials.email, credentials.accessCode)
        }
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo regenerar la clave de acceso.", cause = it) },
    )

    override suspend fun updateClientComponents(
        clientId: String,
        components: Set<ClientPortalComponent>,
    ): AppResult<Client> = runCatching {
        dataSource.updateClientComponents(
            clientId = clientId,
            request = UpdateClientComponentsRequestDto(components.map { it.name }.sorted()),
        ).let(ClientsMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudieron actualizar los componentes del cliente.", cause = it) },
    )

    override suspend fun deleteClient(clientId: String): AppResult<Unit> = runCatching {
        dataSource.deleteClient(clientId)
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo borrar el cliente.", cause = it) },
    )
}
