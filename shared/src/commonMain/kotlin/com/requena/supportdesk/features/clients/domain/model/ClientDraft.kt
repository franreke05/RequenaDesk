package com.requena.supportdesk.features.clients.domain.model

import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.PreferredContactChannel

data class ClientDraft(
    val companyName: String,
    val productName: String,
    val contactName: String,
    val email: String,
    val accountStatus: ClientAccountStatus = ClientAccountStatus.ACTIVE,
    val serviceTier: ClientServiceTier = ClientServiceTier.STANDARD,
    val preferredContactChannel: PreferredContactChannel = PreferredContactChannel.TICKET,
)
