package com.requena.supportdesk.features.clients.data.mapper

import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.PreferredContactChannel
import com.requena.supportdesk.features.clients.data.dto.ClientDto

object ClientsMapper {
    fun fromDto(dto: ClientDto): Client = Client(
        id = dto.id,
        companyName = dto.companyName,
        productName = dto.productName,
        contactName = dto.contactName.ifBlank { dto.companyName },
        email = dto.email,
        accountStatus = ClientAccountStatus.valueOf(dto.accountStatus),
        serviceTier = ClientServiceTier.valueOf(dto.serviceTier),
        preferredContactChannel = PreferredContactChannel.valueOf(dto.preferredContactChannel),
        activeTicketCount = dto.activeTicketCount,
    )
}
