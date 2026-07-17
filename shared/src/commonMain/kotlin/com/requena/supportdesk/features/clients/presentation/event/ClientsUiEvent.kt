package com.requena.supportdesk.features.clients.presentation.event

import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.ClientPortalComponent
import com.requena.supportdesk.core.model.PreferredContactChannel

sealed interface ClientsUiEvent {
    object Load : ClientsUiEvent
    data class SearchChanged(val query: String) : ClientsUiEvent
    data class SelectClient(val clientId: String) : ClientsUiEvent
    data class CreateClient(
        val companyName: String,
        val productName: String,
        val contactName: String,
        val email: String,
        val accountStatus: ClientAccountStatus = ClientAccountStatus.ACTIVE,
        val serviceTier: ClientServiceTier = ClientServiceTier.STANDARD,
        val preferredContactChannel: PreferredContactChannel = PreferredContactChannel.TICKET,
    ) : ClientsUiEvent
    data class UpdateClient(
        val clientId: String,
        val companyName: String,
        val productName: String,
        val contactName: String,
        val email: String,
        val accountStatus: ClientAccountStatus,
        val serviceTier: ClientServiceTier,
        val preferredContactChannel: PreferredContactChannel,
    ) : ClientsUiEvent
    data class RegenerateClientCredentials(val clientId: String) : ClientsUiEvent
    object DismissGeneratedCredentials : ClientsUiEvent
    data class UpdateClientComponents(
        val clientId: String,
        val components: Set<ClientPortalComponent>,
    ) : ClientsUiEvent
    data class DeleteClient(val clientId: String) : ClientsUiEvent
    data class AddClientNote(
        val clientId: String,
        val body: String,
        val authorId: String,
        val authorName: String,
    ) : ClientsUiEvent
}
